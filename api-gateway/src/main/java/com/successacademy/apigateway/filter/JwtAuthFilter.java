package com.successacademy.apigateway.filter;

import com.successacademy.apigateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    /**
     * Public paths — accessible WITHOUT a JWT token.
     * All other paths require Authorization: Bearer <token>
     */
    private static final List<String> PUBLIC_PATHS = List.of(
            // Auth
            "/auth-service/auth/login",
            "/auth-service/auth/register",
            "/auth-service/auth/exists",
            "/auth-service/auth/change-password",

            // Public notices (homepage + notices page)
            "/notice-service/notice/public",

            // Public events (homepage + events page)
            "/event-service/event/public",

            // Contact form submission (public contact page)
            "/contact-service/contact/submit",

            // Public fee structure (fees page)
            "/fee-service/fees/structure",

            // Public faculty listing (faculty page)
            "/faculty-service/faculty/public",

            // Public gallery (gallery page: list + category filter)
            "/gallery-service/gallery/public",

            // Public uploaded gallery images (served by gallery-service)
            "/gallery-service/gallery/uploads",

            // Static uploaded images (photos, avatars)
            "/student-service/uploads",
            "/faculty-service/uploads"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // Allow public paths without token
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::contains);
        if (isPublic) {
            return chain.filter(exchange);
        }

        // Extract Bearer token from Authorization header or from query param for WebSocket handshake
        String token = null;
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        } else if (path.contains("/communication-service/ws")) {
            // WebSocket STOMP/SockJS handshake query param support
            String paramToken = exchange.getRequest().getQueryParams().getFirst("token");
            if (paramToken != null && !paramToken.isBlank()) {
                token = paramToken.trim();
            }
        }

        // 1. Support Mock / Demo tokens in development
        if (token != null && token.startsWith("mock-token-")) {
            String uname = token.substring(11).toLowerCase();
            String role = "STUDENT";
            Long uid = 101L;
            Long sId = 55L;
            Long tId = null;

            if (uname.contains("admin")) {
                role = "ADMIN";
                uid = 1L;
                sId = null;
            } else if (uname.contains("teacher") || uname.contains("faculty") || uname.contains("sharma") || uname.contains("priya")) {
                role = "TEACHER";
                uid = 201L;
                tId = 12L;
                sId = null;
            }

            final String finalRole = role;
            final Long finalUid = uid;
            final Long finalSId = sId;
            final Long finalTId = tId;

            org.springframework.http.server.reactive.ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.remove("X-User-Id");
                        httpHeaders.remove("X-User-Role");
                        httpHeaders.remove("X-Username");
                        httpHeaders.remove("X-Student-Id");
                        httpHeaders.remove("X-Teacher-Id");

                        httpHeaders.set("X-Username", uname);
                        httpHeaders.set("X-User-Role", finalRole);
                        httpHeaders.set("X-User-Id", String.valueOf(finalUid));
                        if (finalSId != null) httpHeaders.set("X-Student-Id", String.valueOf(finalSId));
                        if (finalTId != null) httpHeaders.set("X-Teacher-Id", String.valueOf(finalTId));
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // 2. Real JWT Token Validation
        if (token != null && !token.isBlank()) {
            io.jsonwebtoken.Claims claims = jwtUtil.getClaims(token);
            if (claims != null && claims.getSubject() != null) {
                String username = claims.getSubject();
                String role = claims.get("role", String.class);
                Object userIdObj = claims.get("userId");
                Object studentIdObj = claims.get("studentId");
                Object teacherIdObj = claims.get("teacherId");

                org.springframework.http.server.reactive.ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .headers(httpHeaders -> {
                            httpHeaders.remove("X-User-Id");
                            httpHeaders.remove("X-User-Role");
                            httpHeaders.remove("X-Username");
                            httpHeaders.remove("X-Student-Id");
                            httpHeaders.remove("X-Teacher-Id");

                            httpHeaders.set("X-Username", username != null ? username : "");
                            if (role != null) httpHeaders.set("X-User-Role", role);
                            if (userIdObj != null) httpHeaders.set("X-User-Id", String.valueOf(userIdObj));
                            if (studentIdObj != null) httpHeaders.set("X-Student-Id", String.valueOf(studentIdObj));
                            if (teacherIdObj != null) httpHeaders.set("X-Teacher-Id", String.valueOf(teacherIdObj));
                        })
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            }
        }

        // 3. Allow Public paths without token
        if (isPublic) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap("Missing or invalid Authorization header".getBytes())));
    }

    @Override
    public int getOrder() {
        return -1; // Runs before all other filters
    }
}
