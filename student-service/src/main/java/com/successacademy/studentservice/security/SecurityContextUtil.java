package com.successacademy.studentservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityContextUtil {

    @Value("${app.gateway.internal-secret:${GATEWAY_INTERNAL_SECRET:success-academy-secure-internal-gateway-token-2026}}")
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

    public Long getCurrentTeacherId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        return parseLong(req.getHeader("X-Teacher-Id"));
    }

    public Long getCurrentStudentId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        return parseLong(req.getHeader("X-Student-Id"));
    }

    public Long getCurrentGuardianId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        return parseLong(req.getHeader("X-Guardian-Id"));
    }

    public String getCurrentUsername(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return "system";
        String uname = req.getHeader("X-Username");
        return (uname != null && !uname.isBlank()) ? uname.trim() : "system";
    }

    public String getTenantId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return TenantContext.DEFAULT_TENANT;
        String tenant = req.getHeader("X-Tenant-Id");
        return (tenant != null && !tenant.isBlank()) ? tenant.trim() : TenantContext.DEFAULT_TENANT;
    }

    public boolean isAdmin(HttpServletRequest req) {
        String role = getCurrentRole(req);
        return "ADMIN".equalsIgnoreCase(role)
                || "TENANT_ADMIN".equalsIgnoreCase(role)
                || "PLATFORM_ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role)
                || "ADMIN_DELEGATE".equalsIgnoreCase(role)
                || "PRINCIPAL".equalsIgnoreCase(role);
    }

    public void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator privilege required.");
        }
    }

    public void requireAuthenticated(HttpServletRequest req) {
        if ("ANONYMOUS".equalsIgnoreCase(getCurrentRole(req))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
    }

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            long parsed = Long.parseLong(val.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
