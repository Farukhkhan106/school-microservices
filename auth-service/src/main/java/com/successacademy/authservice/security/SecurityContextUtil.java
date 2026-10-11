package com.successacademy.authservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityContextUtil {

    @Value("${app.gateway.internal-secret:${INTERNAL_GATEWAY_SECRET:success-academy-secure-internal-gateway-token-2026}}")
    private String internalGatewaySecret;

    private final JwtUtil jwtUtil;

    public SecurityContextUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public boolean isTrustedInternalCall(HttpServletRequest req) {
        String secret = req.getHeader("X-Internal-Secret");
        return secret != null && secret.equals(internalGatewaySecret);
    }

    public String getCurrentRole(HttpServletRequest req) {
        if (isTrustedInternalCall(req)) {
            String role = req.getHeader("X-User-Role");
            if (role != null && !role.isBlank()) {
                String clean = role.trim().toUpperCase();
                if (clean.startsWith("ROLE_")) clean = clean.substring(5);
                return clean;
            }
        }
        io.jsonwebtoken.Claims claims = extractBearerClaims(req);
        if (claims != null && claims.get("role") != null) {
            String clean = String.valueOf(claims.get("role")).trim().toUpperCase();
            if (clean.startsWith("ROLE_")) clean = clean.substring(5);
            return clean;
        }
        return "ANONYMOUS";
    }

    public Long getCurrentUserId(HttpServletRequest req) {
        if (isTrustedInternalCall(req)) {
            Long uid = parseLong(req.getHeader("X-User-Id"));
            if (uid != null) return uid;
        }
        io.jsonwebtoken.Claims claims = extractBearerClaims(req);
        if (claims != null && claims.get("userId") != null) {
            return parseLong(String.valueOf(claims.get("userId")));
        }
        return null;
    }

    public String getCurrentUsername(HttpServletRequest req) {
        if (isTrustedInternalCall(req)) {
            String uname = req.getHeader("X-Username");
            if (uname != null && !uname.isBlank()) return uname.trim();
        }
        io.jsonwebtoken.Claims claims = extractBearerClaims(req);
        if (claims != null && claims.getSubject() != null) {
            return claims.getSubject().trim();
        }
        return null;
    }

    public String getTenantId(HttpServletRequest req) {
        if (isTrustedInternalCall(req)) {
            String tenant = req.getHeader("X-Tenant-Id");
            if (tenant != null && !tenant.isBlank()) return tenant.trim();
        }
        io.jsonwebtoken.Claims claims = extractBearerClaims(req);
        if (claims != null && claims.get("tenantId") != null) {
            String t = String.valueOf(claims.get("tenantId")).trim();
            if (!t.isBlank()) return t;
        }
        return TenantContext.DEFAULT_TENANT;
    }

    private io.jsonwebtoken.Claims extractBearerClaims(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return jwtUtil.extractClaims(token);
        }
        return null;
    }

    public boolean isAdmin(HttpServletRequest req) {
        String role = getCurrentRole(req);
        return "ADMIN".equalsIgnoreCase(role)
                || "PLATFORM_ADMIN".equalsIgnoreCase(role)
                || "TENANT_ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    public void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required.");
        }
    }

    public void requireAuthenticated(HttpServletRequest req) {
        if ("ANONYMOUS".equalsIgnoreCase(getCurrentRole(req)) || getCurrentUserId(req) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
