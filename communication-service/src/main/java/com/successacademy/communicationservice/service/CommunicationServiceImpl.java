package com.successacademy.communicationservice.service;

import com.successacademy.communicationservice.client.AuthServiceClient;
import com.successacademy.communicationservice.client.FacultyServiceClient;
import com.successacademy.communicationservice.client.StudentServiceClient;
import com.successacademy.communicationservice.dto.*;
import com.successacademy.communicationservice.model.CommunicationAuditLog;
import com.successacademy.communicationservice.model.Conversation;
import com.successacademy.communicationservice.model.ConversationParticipant;
import com.successacademy.communicationservice.model.Message;
import com.successacademy.communicationservice.repository.CommunicationAuditLogRepository;
import com.successacademy.communicationservice.repository.ConversationParticipantRepository;
import com.successacademy.communicationservice.repository.ConversationRepository;
import com.successacademy.communicationservice.repository.MessageRepository;
import com.successacademy.communicationservice.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationServiceImpl implements CommunicationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final CommunicationAuditLogRepository auditLogRepository;
    private final SchoolRelationshipService relationshipService;
    private final AuthServiceClient authServiceClient;
    private final StudentServiceClient studentServiceClient;
    private final FacultyServiceClient facultyServiceClient;
    private final SimpMessagingTemplate messagingTemplate;

    // ─────────────────────────────────────────────────────────────
    //  SYSTEM & CLASS CHANNEL AUTO-SYNC
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public synchronized void ensureSystemChannels() {
        // 1. School Official Announcements
        if (conversationRepository.findFirstByTypeAndActiveTrue("BROADCAST").isEmpty()) {
            Conversation announcements = Conversation.builder()
                    .type("BROADCAST")
                    .title("School Official Announcements")
                    .targetRole("ALL")
                    .createdBy(1L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .active(true)
                    .build();
            conversationRepository.save(announcements);
            log.info("✅ Created system channel: School Official Announcements");
        }

        // 2. Faculty Staff Room
        if (conversationRepository.findFirstByTypeAndActiveTrue("STAFF_GROUP").isEmpty()) {
            Conversation staffRoom = Conversation.builder()
                    .type("STAFF_GROUP")
                    .title("Faculty Staff Room")
                    .targetRole("TEACHER")
                    .createdBy(1L)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .active(true)
                    .build();
            conversationRepository.save(staffRoom);
            log.info("✅ Created system channel: Faculty Staff Room");
        }

        // 3. Dynamic Class Channels derived from REAL school data (Requirement 2 & 16)
        // Only create for (class, section) pairs where active students actually exist!
        Set<String> realClasses = relationshipService.getRealActiveClasses();
        for (String classSec : realClasses) {
            String[] parts = classSec.split("-");
            String sClass = parts[0];
            String sec = parts.length > 1 ? parts[1] : "A";

            Optional<Conversation> existing = conversationRepository.findByTypeAndTargetClassAndTargetSection(
                    "CLASS_GROUP", sClass, sec
            );
            if (existing.isEmpty()) {
                Conversation classChannel = Conversation.builder()
                        .type("CLASS_GROUP")
                        .title("Class " + sClass + "-" + sec)
                        .targetClass(sClass)
                        .targetSection(sec)
                        .createdBy(1L)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .active(true)
                        .build();
                conversationRepository.save(classChannel);
                log.info("✅ Created real school class channel: Class {}-{}", sClass, sec);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  GET USER CONVERSATIONS
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public List<ConversationDto> getUserConversations(UserContext user) {
        user = relationshipService.enrichUserContext(user);
        ensureSystemChannels();

        List<Conversation> allActive = conversationRepository.findAll();
        List<ConversationDto> result = new ArrayList<>();

        for (Conversation c : allActive) {
            if (!c.isActive()) continue;

            boolean authorized = false;

            switch (c.getType()) {
                case "BROADCAST":
                    // All users can see School Announcements
                    authorized = true;
                    break;
                case "STAFF_GROUP":
                    // Only Admin and Teachers
                    authorized = relationshipService.canAccessStaffRoom(user);
                    break;
                case "CLASS_GROUP":
                    // Only Admin, assigned Teachers, or students of that class
                    authorized = relationshipService.canAccessClassChannel(user, c.getTargetClass(), c.getTargetSection());
                    break;
                case "DIRECT":
                    // Only if user is one of the participants
                    authorized = participantRepository.existsByConversationIdAndUserId(c.getId(), user.getUserId());
                    break;
            }

            if (authorized) {
                result.add(mapToConversationDto(c, user));
            }
        }

        // Sort: latest activity first
        result.sort((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
        return result;
    }

    @Override
    public ConversationDto getConversationById(Long conversationId, UserContext user) {
        user = relationshipService.enrichUserContext(user);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        verifyAccess(conv, user);
        return mapToConversationDto(conv, user);
    }

    // ─────────────────────────────────────────────────────────────
    //  MESSAGES HISTORY (PAGINATED)
    // ─────────────────────────────────────────────────────────────

    @Override
    public Page<MessageDto> getConversationMessages(Long conversationId, UserContext user, int page, int size) {
        user = relationshipService.enrichUserContext(user);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        verifyAccess(conv, user);

        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("createdAt").descending());
        Page<Message> msgPage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageRequest);

        return msgPage.map(this::mapToMessageDto);
    }

    // ─────────────────────────────────────────────────────────────
    //  SEND MESSAGE
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MessageDto sendMessage(Long conversationId, UserContext user, SendMessageRequest request) {
        user = relationshipService.enrichUserContext(user);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        verifyAccess(conv, user);

        // Verify post permissions (e.g. only ADMIN can post in BROADCAST)
        if (!relationshipService.canPostMessage(user, conv.getType(), conv.getTargetClass(), conv.getTargetSection())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to post in this conversation");
        }

        // 1. Persist to DB first (Requirement 15: Persist BEFORE delivery)
        Message msg = Message.builder()
                .conversationId(conversationId)
                .senderId(user.getUserId())
                .senderRole(user.getRole())
                .senderUsername(user.getUsername())
                .content(request.getContent().trim())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Message saved = messageRepository.save(msg);

        // Update conversation timestamp
        conv.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conv);

        // Auto-mark as read for sender
        updateParticipantLastRead(conv.getId(), user.getUserId(), user.getRole(), user.getUsername(), saved.getId());

        // Audit Log
        auditLogRepository.save(CommunicationAuditLog.builder()
                .action("SEND_MESSAGE")
                .performedBy(user.getUserId())
                .performedByRole(user.getRole())
                .targetType("MESSAGE")
                .targetId(saved.getId())
                .details("Message sent in conv: " + conversationId + " (" + conv.getType() + ")")
                .timestamp(LocalDateTime.now())
                .build());

        MessageDto dto = mapToMessageDto(saved);

        // 2. Publish to STOMP destination: /topic/conversation.{id}
        try {
            messagingTemplate.convertAndSend("/topic/conversation." + conversationId, dto);
        } catch (Exception e) {
            log.warn("Failed to broadcast message over WebSocket: {}", e.getMessage());
        }

        return dto;
    }

    // ─────────────────────────────────────────────────────────────
    //  CREATE CONVERSATION (STRICT ROLE & RELATIONSHIP VALIDATION)
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConversationDto createConversation(UserContext user, CreateConversationRequest request) {
        user = relationshipService.enrichUserContext(user);

        if ("DIRECT".equalsIgnoreCase(request.getType())) {
            Long recipientId = request.getRecipientUserId();
            if (recipientId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient user ID is required for direct conversation");
            }

            UserContext recipient = authServiceClient.getUserById(recipientId);
            if (recipient == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient user not found");
            }
            recipient = relationshipService.enrichUserContext(recipient);

            // STRICT RELATIONSHIP CHECK (Requirements 4, 5, 6)
            if (!relationshipService.canInitiateDirectConversation(user, recipient)) {
                log.warn("Forbidden DIRECT conversation attempt between {} ({}) and {} ({})",
                        user.getUsername(), user.getRole(), recipient.getUsername(), recipient.getRole());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Direct communication between your role and this user is not authorized");
            }

            // Check if DIRECT conversation already exists between these two users
            Optional<Conversation> existing = conversationRepository.findDirectConversationBetween(user.getUserId(), recipient.getUserId());
            if (existing.isPresent()) {
                Conversation c = existing.get();
                if (request.getInitialMessage() != null && !request.getInitialMessage().isBlank()) {
                    sendMessage(c.getId(), user, new SendMessageRequest(request.getInitialMessage(), "TEXT", null, null));
                }
                return mapToConversationDto(c, user);
            }

            // Create new DIRECT conversation
            String title = resolveDisplayName(recipient);
            Conversation conv = Conversation.builder()
                    .type("DIRECT")
                    .title(title)
                    .createdBy(user.getUserId())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .active(true)
                    .build();

            Conversation savedConv = conversationRepository.save(conv);

            // Add both participants
            participantRepository.save(ConversationParticipant.builder()
                    .conversationId(savedConv.getId())
                    .userId(user.getUserId())
                    .userRole(user.getRole())
                    .username(user.getUsername())
                    .joinedAt(LocalDateTime.now())
                    .build());

            participantRepository.save(ConversationParticipant.builder()
                    .conversationId(savedConv.getId())
                    .userId(recipient.getUserId())
                    .userRole(recipient.getRole())
                    .username(recipient.getUsername())
                    .joinedAt(LocalDateTime.now())
                    .build());

            auditLogRepository.save(CommunicationAuditLog.builder()
                    .action("CREATE_CONVERSATION")
                    .performedBy(user.getUserId())
                    .performedByRole(user.getRole())
                    .targetType("CONVERSATION")
                    .targetId(savedConv.getId())
                    .details("Created DIRECT conversation with user " + recipient.getUserId())
                    .timestamp(LocalDateTime.now())
                    .build());

            if (request.getInitialMessage() != null && !request.getInitialMessage().isBlank()) {
                sendMessage(savedConv.getId(), user, new SendMessageRequest(request.getInitialMessage(), "TEXT", null, null));
            }

            return mapToConversationDto(savedConv, user);
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported conversation creation type: " + request.getType());
    }

    // ─────────────────────────────────────────────────────────────
    //  MARK AS READ
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void markAsRead(Long conversationId, UserContext user) {
        user = relationshipService.enrichUserContext(user);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        verifyAccess(conv, user);

        Optional<Message> latest = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId);
        Long lastMsgId = latest.map(Message::getId).orElse(0L);

        updateParticipantLastRead(conversationId, user.getUserId(), user.getRole(), user.getUsername(), lastMsgId);
    }

    // ─────────────────────────────────────────────────────────────
    //  UNREAD COUNTS
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UserContext user) {
        user = relationshipService.enrichUserContext(user);
        List<ConversationDto> convs = getUserConversations(user);

        long total = 0;
        Map<Long, Long> byConv = new HashMap<>();

        for (ConversationDto c : convs) {
            long count = c.getUnreadCount();
            if (count > 0) {
                total += count;
                byConv.put(c.getId(), count);
            }
        }

        return new UnreadCountResponse(total, byConv);
    }

    // ─────────────────────────────────────────────────────────────
    //  ADMIN BROADCAST (Requirement 7 & 18)
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MessageDto broadcastAnnouncement(UserContext user, BroadcastRequest request) {
        final UserContext currentUser = relationshipService.enrichUserContext(user);

        // Strict Server-Side Check: ONLY ADMIN can broadcast
        if (!currentUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Administrators can send school broadcasts");
        }

        Conversation announcements = conversationRepository.findFirstByTypeAndActiveTrue("BROADCAST")
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .type("BROADCAST")
                            .title("School Official Announcements")
                            .targetRole("ALL")
                            .createdBy(currentUser.getUserId())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .active(true)
                            .build();
                    return conversationRepository.save(c);
                });

        Message msg = Message.builder()
                .conversationId(announcements.getId())
                .senderId(currentUser.getUserId())
                .senderRole("ADMIN")
                .senderUsername(currentUser.getUsername())
                .content(request.getContent().trim())
                .messageType("ANNOUNCEMENT")
                .createdAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        Message saved = messageRepository.save(msg);

        announcements.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(announcements);

        auditLogRepository.save(CommunicationAuditLog.builder()
                .action("BROADCAST")
                .performedBy(user.getUserId())
                .performedByRole(user.getRole())
                .targetType("BROADCAST")
                .targetId(saved.getId())
                .details("Broadcast announcement sent: " + request.getTitle())
                .timestamp(LocalDateTime.now())
                .build());

        MessageDto dto = mapToMessageDto(saved);

        // Push to WebSocket announcements topic
        try {
            messagingTemplate.convertAndSend("/topic/conversation." + announcements.getId(), dto);
            messagingTemplate.convertAndSend("/topic/announcements", dto);
        } catch (Exception e) {
            log.warn("WebSocket push failed for announcement: {}", e.getMessage());
        }

        return dto;
    }

    // ─────────────────────────────────────────────────────────────
    //  SOFT-DELETE MESSAGE (Requirement 10)
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteMessage(Long messageId, UserContext user) {
        user = relationshipService.enrichUserContext(user);
        Message msg = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        // Only sender or Admin can delete
        if (!user.isAdmin() && !msg.getSenderId().equals(user.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own messages");
        }

        msg.setDeleted(true);
        msg.setDeletedAt(LocalDateTime.now());
        messageRepository.save(msg);

        auditLogRepository.save(CommunicationAuditLog.builder()
                .action("DELETE_MESSAGE")
                .performedBy(user.getUserId())
                .performedByRole(user.getRole())
                .targetType("MESSAGE")
                .targetId(msg.getId())
                .details("Soft deleted message id: " + messageId)
                .timestamp(LocalDateTime.now())
                .build());

        // Notify subscribers of deleted message
        try {
            MessageDto dto = mapToMessageDto(msg);
            messagingTemplate.convertAndSend("/topic/conversation." + msg.getConversationId(), dto);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────
    //  ELIGIBLE RECIPIENTS (Requirement 23)
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<RecipientDto> getEligibleRecipients(UserContext user) {
        user = relationshipService.enrichUserContext(user);
        List<RecipientDto> recipients = new ArrayList<>();

        if (user.isAdmin()) {
            // Admin can message any teacher or student
            List<FacultyServiceClient.FacultyInfo> facultyList = facultyServiceClient.getAllFaculty();
            for (FacultyServiceClient.FacultyInfo f : facultyList) {
                if (f.getUserId() != null && !f.getUserId().equals(user.getUserId())) {
                    recipients.add(RecipientDto.builder()
                            .userId(f.getUserId())
                            .name(f.getName())
                            .role("TEACHER")
                            .description(f.getDesignation() != null ? f.getDesignation() : "Faculty Member")
                            .referenceId(f.getId())
                            .build());
                }
            }

            List<StudentServiceClient.StudentInfo> students = studentServiceClient.getAllStudents();
            for (StudentServiceClient.StudentInfo s : students) {
                UserContext sUser = authServiceClient.getUserByUsername(s.getEmail() != null && s.getEmail().contains("@") ? s.getEmail().split("@")[0] : ("student" + s.getId()));
                Long sUserId = sUser != null ? sUser.getUserId() : null;
                recipients.add(RecipientDto.builder()
                        .userId(sUserId != null ? sUserId : s.getId())
                        .name(s.getFullName())
                        .role("STUDENT")
                        .description("Class " + s.getClassSectionKey())
                        .referenceId(s.getId())
                        .build());
            }

            return recipients;
        }

        if (user.isTeacher()) {
            // 1. Principal / Admin
            UserContext adminUser = authServiceClient.getUserByUsername("admin");
            if (adminUser != null) {
                recipients.add(RecipientDto.builder()
                        .userId(adminUser.getUserId())
                        .name("Principal / Administrator")
                        .role("ADMIN")
                        .description("School Administration")
                        .build());
            }

            // 2. Fellow teachers
            List<FacultyServiceClient.FacultyInfo> facultyList = facultyServiceClient.getAllFaculty();
            for (FacultyServiceClient.FacultyInfo f : facultyList) {
                if (f.getUserId() != null && !f.getUserId().equals(user.getUserId())) {
                    recipients.add(RecipientDto.builder()
                            .userId(f.getUserId())
                            .name(f.getName())
                            .role("TEACHER")
                            .description(f.getDesignation() != null ? f.getDesignation() : "Faculty Member")
                            .referenceId(f.getId())
                            .build());
                }
            }

            // 3. Students belonging to this teacher's assigned classes
            FacultyServiceClient.FacultyInfo faculty = relationshipService.getFacultyForUser(user);
            if (faculty != null && faculty.getClassTeacherOf() != null) {
                String myClassSec = faculty.getClassTeacherOf().trim();
                List<StudentServiceClient.StudentInfo> allStudents = studentServiceClient.getAllStudents();
                for (StudentServiceClient.StudentInfo s : allStudents) {
                    if (myClassSec.equalsIgnoreCase(s.getClassSectionKey())) {
                        UserContext sUser = authServiceClient.getUserByUsername(s.getEmail() != null && s.getEmail().contains("@") ? s.getEmail().split("@")[0] : ("student" + s.getId()));
                        Long sUserId = sUser != null ? sUser.getUserId() : null;
                        recipients.add(RecipientDto.builder()
                                .userId(sUserId != null ? sUserId : s.getId())
                                .name(s.getFullName())
                                .role("STUDENT")
                                .description("Class " + s.getClassSectionKey())
                                .referenceId(s.getId())
                                .build());
                    }
                }
            }
            return recipients;
        }

        if (user.isStudent()) {
            // Student can ONLY message:
            // 1. Principal / Admin
            UserContext adminUser = authServiceClient.getUserByUsername("admin");
            if (adminUser != null) {
                recipients.add(RecipientDto.builder()
                        .userId(adminUser.getUserId())
                        .name("Principal / Administrator")
                        .role("ADMIN")
                        .description("School Inquiries & Administration")
                        .build());
            }

            // 2. Class Teacher & assigned Subject Teachers (NO other students!)
            StudentServiceClient.StudentInfo student = relationshipService.getStudentForUser(user);
            if (student != null) {
                String myClassSec = student.getClassSectionKey();
                List<FacultyServiceClient.FacultyInfo> facultyList = facultyServiceClient.getAllFaculty();
                for (FacultyServiceClient.FacultyInfo f : facultyList) {
                    if (f.getUserId() != null && (f.isPrincipal() || (f.getClassTeacherOf() != null && f.getClassTeacherOf().trim().equalsIgnoreCase(myClassSec)))) {
                        recipients.add(RecipientDto.builder()
                                .userId(f.getUserId())
                                .name(f.getName())
                                .role("TEACHER")
                                .description(f.getClassTeacherOf() != null && f.getClassTeacherOf().equalsIgnoreCase(myClassSec) ? "Class Teacher (" + myClassSec + ")" : f.getDesignation())
                                .referenceId(f.getId())
                                .build());
                    }
                }
            }
            return recipients;
        }

        return List.of();
    }

    // ─────────────────────────────────────────────────────────────
    //  SEARCH MESSAGES
    // ─────────────────────────────────────────────────────────────

    @Override
    public List<MessageDto> searchMessages(Long conversationId, String query, UserContext user) {
        user = relationshipService.enrichUserContext(user);
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        verifyAccess(conv, user);

        List<Message> matches = messageRepository.searchMessages(conversationId, query);
        return matches.stream().map(this::mapToMessageDto).toList();
    }

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────

    private void verifyAccess(Conversation conv, UserContext user) {
        boolean authorized = false;

        switch (conv.getType()) {
            case "BROADCAST":
                authorized = true; // All authenticated users can read announcements
                break;
            case "STAFF_GROUP":
                authorized = relationshipService.canAccessStaffRoom(user);
                break;
            case "CLASS_GROUP":
                authorized = relationshipService.canAccessClassChannel(user, conv.getTargetClass(), conv.getTargetSection());
                break;
            case "DIRECT":
                authorized = participantRepository.existsByConversationIdAndUserId(conv.getId(), user.getUserId());
                break;
        }

        if (!authorized) {
            log.warn("Access DENIED: User {} ({}) attempted to access conv {} ({})",
                    user.getUsername(), user.getRole(), conv.getId(), conv.getType());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this conversation");
        }
    }

    private void updateParticipantLastRead(Long convId, Long userId, String role, String username, Long lastMsgId) {
        Optional<ConversationParticipant> opt = participantRepository.findByConversationIdAndUserId(convId, userId);
        ConversationParticipant p;
        if (opt.isPresent()) {
            p = opt.get();
        } else {
            p = ConversationParticipant.builder()
                    .conversationId(convId)
                    .userId(userId)
                    .userRole(role)
                    .username(username)
                    .joinedAt(LocalDateTime.now())
                    .build();
        }
        p.setLastReadMessageId(lastMsgId);
        p.setLastReadAt(LocalDateTime.now());
        participantRepository.save(p);
    }

    private ConversationDto mapToConversationDto(Conversation c, UserContext currentUser) {
        Optional<Message> lastMsgOpt = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(c.getId());
        MessageDto lastMsg = lastMsgOpt.map(this::mapToMessageDto).orElse(null);

        // Calculate unread count for current user
        Optional<ConversationParticipant> partOpt = participantRepository.findByConversationIdAndUserId(c.getId(), currentUser.getUserId());
        Long lastReadId = partOpt.map(ConversationParticipant::getLastReadMessageId).orElse(null);

        long unread = messageRepository.countUnreadMessages(c.getId(), lastReadId, currentUser.getUserId());

        ConversationDto dto = ConversationDto.builder()
                .id(c.getId())
                .type(c.getType())
                .title(c.getTitle())
                .targetClass(c.getTargetClass())
                .targetSection(c.getTargetSection())
                .targetRole(c.getTargetRole())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .active(c.isActive())
                .unreadCount(unread)
                .lastMessage(lastMsg)
                .build();

        // For DIRECT conversations, add the other participant's details
        if ("DIRECT".equalsIgnoreCase(c.getType())) {
            List<ConversationParticipant> parts = participantRepository.findByConversationId(c.getId());
            for (ConversationParticipant p : parts) {
                if (!p.getUserId().equals(currentUser.getUserId())) {
                    dto.setOtherUserId(p.getUserId());
                    dto.setOtherUsername(p.getUsername());
                    dto.setOtherUserRole(p.getUserRole());
                    dto.setOtherDisplayName(p.getUsername());
                    break;
                }
            }
        }

        return dto;
    }

    private MessageDto mapToMessageDto(Message m) {
        String content = m.isDeleted() ? "This message was deleted" : m.getContent();
        return MessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .senderRole(m.getSenderRole())
                .senderUsername(m.getSenderUsername())
                .content(content)
                .messageType(m.getMessageType())
                .attachmentUrl(m.isDeleted() ? null : m.getAttachmentUrl())
                .attachmentName(m.isDeleted() ? null : m.getAttachmentName())
                .createdAt(m.getCreatedAt())
                .editedAt(m.getEditedAt())
                .isDeleted(m.isDeleted())
                .build();
    }

    private String resolveDisplayName(UserContext user) {
        if (user.isAdmin()) return "School Administration";
        if (user.isTeacher()) {
            FacultyServiceClient.FacultyInfo f = relationshipService.getFacultyForUser(user);
            if (f != null && f.getName() != null) return f.getName();
        }
        if (user.isStudent()) {
            StudentServiceClient.StudentInfo s = relationshipService.getStudentForUser(user);
            if (s != null) return s.getFullName();
        }
        return user.getUsername();
    }
}
