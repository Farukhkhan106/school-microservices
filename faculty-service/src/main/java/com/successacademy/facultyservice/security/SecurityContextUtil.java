package com.successacademy.facultyservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityContextUtil {

    @org.springframework.beans.factory.annotation.Value("${app.gateway.internal-secret:${INTERNAL_GATEWAY_SECRET:success-academy-secure-internal-gateway-token-2026}}")
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
        String idStr = req.getHeader("X-User-Id");
        return parseLong(idStr);
    }

    public String getCurrentUsername(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        String uname = req.getHeader("X-Username");
        return uname != null ? uname.trim() : null;
    }

    public String getTenantId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return "default";
        String tenant = req.getHeader("X-Tenant-Id");
        return tenant != null && !tenant.isBlank() ? tenant.trim() : "default";
    }

    public boolean isAdmin(HttpServletRequest req) {
        String role = getCurrentRole(req);
        return "ADMIN".equals(role) || "SUPPORT".equals(role);
    }

    public void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator or Support role required.");
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
