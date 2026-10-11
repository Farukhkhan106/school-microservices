package com.successacademy.authservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenantContextFilter extends OncePerRequestFilter {

    private final SecurityContextUtil securityContextUtil;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String tenantId = null;

            // 1. Check trusted gateway header first
            if (securityContextUtil.isTrustedInternalCall(request)) {
                String headerTenant = request.getHeader("X-Tenant-Id");
                if (headerTenant != null && !headerTenant.isBlank()) {
                    tenantId = headerTenant.trim();
                }
            }

            // 2. If not set, extract tenantId from valid JWT Bearer token if present
            if (tenantId == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7).trim();
                    Claims claims = jwtUtil.extractClaims(token);
                    if (claims != null) {
                        String tokenTenant = claims.get("tenantId", String.class);
                        if (tokenTenant != null && !tokenTenant.isBlank()) {
                            tenantId = tokenTenant.trim();
                        }
                    }
                }
            }

            // 3. Fallback to default
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = TenantContext.DEFAULT_TENANT;
            }

            TenantContext.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
