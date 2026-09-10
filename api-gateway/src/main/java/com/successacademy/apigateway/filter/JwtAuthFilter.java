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

        // All other paths require a valid Bearer token
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap("Missing or invalid Authorization header".getBytes())));
        }

        String token = authHeader.substring(7);

        if (jwtUtil.validateToken(token) == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap("Invalid or expired token".getBytes())));
        }

        // Forward the authenticated identity to downstream services so THEY can
        // enforce role/ownership rules (services must never trust client input).
        ServerWebExchange mutated = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-User-Id", String.valueOf(jwtUtil.extractUserId(token)))
                        .header("X-User-Role", jwtUtil.extractRole(token) == null ? "" : jwtUtil.extractRole(token))
                        .header("X-Student-Id", String.valueOf(jwtUtil.extractStudentId(token)))
                        .build())
                .build();

        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -1; // Runs before all other filters
    }
}
