package com.successacademy.communicationservice.websocket;

import com.successacademy.communicationservice.client.AuthServiceClient;
import com.successacademy.communicationservice.model.Conversation;
import com.successacademy.communicationservice.repository.ConversationParticipantRepository;
import com.successacademy.communicationservice.repository.ConversationRepository;
import com.successacademy.communicationservice.security.JwtTokenService;
import com.successacademy.communicationservice.security.StompPrincipal;
import com.successacademy.communicationservice.security.UserContext;
import com.successacademy.communicationservice.service.SchoolRelationshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenService jwtTokenService;
    private final AuthServiceClient authServiceClient;
    private final SchoolRelationshipService relationshipService;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();

        // ─────────────────────────────────────────────────────────
        // 1. STOMP CONNECT: AUTHENTICATE JWT & SET PRINCIPAL
        // ─────────────────────────────────────────────────────────
        if (StompCommand.CONNECT.equals(command)) {
            String token = extractToken(accessor);

            if (token == null || token.isBlank()) {
                log.warn("STOMP CONNECT rejected: No token provided");
                throw new IllegalArgumentException("Authentication token required for WebSocket connection");
            }

            UserContext ctx = jwtTokenService.extractUserContext(token);
            if (ctx == null || ctx.getUsername() == null) {
                log.warn("STOMP CONNECT rejected: Invalid or expired token");
                throw new IllegalArgumentException("Invalid or expired authentication token");
            }

            // Enrich user ID if missing
            if (ctx.getUserId() == null) {
                UserContext remote = authServiceClient.getUserByUsername(ctx.getUsername());
                if (remote != null) {
                    ctx.setUserId(remote.getUserId());
                    if (ctx.getStudentId() == null) ctx.setStudentId(remote.getStudentId());
                    if (ctx.getTeacherId() == null) ctx.setTeacherId(remote.getTeacherId());
                }
            }

            ctx = relationshipService.enrichUserContext(ctx);

            StompPrincipal principal = new StompPrincipal(
                    ctx.getUserId(),
                    ctx.getUsername(),
                    ctx.getRole(),
                    ctx.getStudentId(),
                    ctx.getTeacherId()
            );

            accessor.setUser(principal);
            log.info("✅ STOMP user authenticated: {} (role: {}, userId: {})", ctx.getUsername(), ctx.getRole(), ctx.getUserId());
        }

        // ─────────────────────────────────────────────────────────
        // 2. STOMP SUBSCRIBE: STRICT TOPIC AUTHORIZATION
        // ─────────────────────────────────────────────────────────
        else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            StompPrincipal user = (StompPrincipal) accessor.getUser();

            if (user == null) {
                throw new SecurityException("Unauthenticated subscription attempt");
            }

            if (destination != null) {
                authorizeSubscription(destination, user);
            }
        }

        return message;
    }

    private void authorizeSubscription(String destination, StompPrincipal user) {
        UserContext ctx = UserContext.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole())
                .studentId(user.getStudentId())
                .teacherId(user.getTeacherId())
                .build();
        ctx = relationshipService.enrichUserContext(ctx);

        // A. /topic/announcements -> all authenticated users allowed
        if ("/topic/announcements".equalsIgnoreCase(destination)) {
            return;
        }

        // B. /topic/staff -> ADMIN and TEACHER only
        if ("/topic/staff".equalsIgnoreCase(destination)) {
            if (!relationshipService.canAccessStaffRoom(ctx)) {
                log.warn("BLOCKED: User {} ({}) attempted unauthorized subscription to /topic/staff", user.getUsername(), user.getRole());
                throw new SecurityException("Only faculty and administration can subscribe to staff channel");
            }
            return;
        }

        // C. /topic/conversation.{id}
        if (destination.startsWith("/topic/conversation.")) {
            String idStr = destination.substring("/topic/conversation.".length());
            try {
                Long convId = Long.parseLong(idStr);
                Conversation conv = conversationRepository.findById(convId).orElse(null);
                if (conv == null) {
                    throw new SecurityException("Conversation does not exist");
                }

                boolean allowed = false;
                switch (conv.getType()) {
                    case "BROADCAST":
                        allowed = true;
                        break;
                    case "STAFF_GROUP":
                        allowed = relationshipService.canAccessStaffRoom(ctx);
                        break;
                    case "CLASS_GROUP":
                        allowed = relationshipService.canAccessClassChannel(ctx, conv.getTargetClass(), conv.getTargetSection());
                        break;
                    case "DIRECT":
                        allowed = participantRepository.existsByConversationIdAndUserId(conv.getId(), ctx.getUserId());
                        break;
                }

                if (!allowed) {
                    log.warn("BLOCKED: User {} ({}) attempted unauthorized subscription to conv {}", user.getUsername(), user.getRole(), convId);
                    throw new SecurityException("You are not authorized to subscribe to conversation " + convId);
                }
            } catch (NumberFormatException e) {
                throw new SecurityException("Invalid conversation destination");
            }
            return;
        }

        // D. /topic/class.{class}.{section}
        if (destination.startsWith("/topic/class.")) {
            String[] parts = destination.split("\\.");
            if (parts.length >= 3) {
                String targetClass = parts[1];
                String targetSection = parts[2];
                if (!relationshipService.canAccessClassChannel(ctx, targetClass, targetSection)) {
                    log.warn("BLOCKED: User {} attempted unauthorized subscription to class {}-{}", user.getUsername(), targetClass, targetSection);
                    throw new SecurityException("You are not authorized to subscribe to class " + targetClass + "-" + targetSection);
                }
            }
        }
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Check native STOMP headers: Authorization, X-Authorization, token
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) return header.substring(7).trim();
            return header.trim();
        }

        List<String> xAuth = accessor.getNativeHeader("X-Authorization");
        if (xAuth != null && !xAuth.isEmpty()) {
            String h = xAuth.get(0);
            if (h.startsWith("Bearer ")) return h.substring(7).trim();
            return h.trim();
        }

        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0).trim();
        }

        // Check session attributes (from HTTP handshake query parameter)
        if (accessor.getSessionAttributes() != null) {
            Object tokenAttr = accessor.getSessionAttributes().get("token");
            if (tokenAttr != null) return tokenAttr.toString().trim();
        }

        return null;
    }
}
