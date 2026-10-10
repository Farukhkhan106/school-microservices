package com.successacademy.communicationservice.security;

import com.successacademy.communicationservice.client.AuthServiceClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class UserContextFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AuthServiceClient authServiceClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            UserContext ctx = resolveUserContext(request);
            if (ctx != null) {
                UserContextHolder.set(ctx);
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    @org.springframework.beans.factory.annotation.Value("${app.gateway.internal-secret:${INTERNAL_GATEWAY_SECRET:${GATEWAY_INTERNAL_SECRET:success-academy-secure-internal-gateway-token-2026}}}")
    private String internalGatewaySecret;

    private UserContext resolveUserContext(HttpServletRequest request) {
        // 1. Check trusted headers injected by API Gateway only if X-Internal-Secret matches
        String internalSecret = request.getHeader("X-Internal-Secret");
        boolean isTrustedInternal = internalSecret != null && internalSecret.equals(internalGatewaySecret);

        String gatewayUsername = request.getHeader("X-Username");
        String gatewayRole = request.getHeader("X-User-Role");
        String gatewayUserId = request.getHeader("X-User-Id");
        String gatewayStudentId = request.getHeader("X-Student-Id");
        String gatewayTeacherId = request.getHeader("X-Teacher-Id");
        String gatewayTenantId = request.getHeader("X-Tenant-Id");

        if (isTrustedInternal && gatewayUsername != null && !gatewayUsername.isBlank()) {
            Long userId = parseLong(gatewayUserId);
            Long studentId = parseLong(gatewayStudentId);
            Long teacherId = parseLong(gatewayTeacherId);

            return UserContext.builder()
                    .userId(userId)
                    .username(gatewayUsername)
                    .role(gatewayRole != null ? gatewayRole.toUpperCase() : "STUDENT")
                    .studentId(studentId)
                    .teacherId(teacherId)
                    .tenantId(gatewayTenantId != null && !gatewayTenantId.isBlank() ? gatewayTenantId.trim() : "default")
                    .build();
        }

        // 2. Direct fallback: check Authorization Bearer token or token query param
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else {
            String param = request.getParameter("token");
            if (param != null && !param.isBlank()) {
                token = param.trim();
            }
        }

        if (token != null) {
            UserContext ctx = jwtTokenService.extractUserContext(token);
            if (ctx != null) {
                // If userId is missing in older token, lookup from auth-service
                if (ctx.getUserId() == null && ctx.getUsername() != null) {
                    UserContext remote = authServiceClient.getUserByUsername(ctx.getUsername());
                    if (remote != null) {
                        ctx.setUserId(remote.getUserId());
                        if (ctx.getStudentId() == null) ctx.setStudentId(remote.getStudentId());
                        if (ctx.getTeacherId() == null) ctx.setTeacherId(remote.getTeacherId());
                    }
                }
                return ctx;
            }
        }

        return null;
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
