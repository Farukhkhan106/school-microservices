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

    public boolean isTrustedInternalCall(HttpServletRequest req) {
        String secret = req.getHeader("X-Internal-Secret");
        return secret != null && secret.equals(internalGatewaySecret);
    }

    public String getCurrentRole(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) {
            return "ANONYMOUS";
        }
        String role = req.getHeader("X-User-Role");
        if (role == null || role.isBlank()) return "ANONYMOUS";
        String clean = role.trim().toUpperCase();
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        return clean;
    }

    public Long getCurrentUserId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        return parseLong(req.getHeader("X-User-Id"));
    }

    public String getCurrentUsername(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        String uname = req.getHeader("X-Username");
        return uname != null ? uname.trim() : null;
    }

    public String getTenantId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) {
            // Check if user is authenticated via Bearer token in request
            return TenantContext.DEFAULT_TENANT;
        }
        String tenant = req.getHeader("X-Tenant-Id");
        return tenant != null && !tenant.isBlank() ? tenant.trim() : TenantContext.DEFAULT_TENANT;
    }

    public boolean isAdmin(HttpServletRequest req) {
        return "ADMIN".equalsIgnoreCase(getCurrentRole(req));
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
