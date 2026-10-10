package com.successacademy.attendanceservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUtil {

    @Value("${app.gateway.internal-secret:${INTERNAL_GATEWAY_SECRET:${GATEWAY_INTERNAL_SECRET:success-academy-secure-internal-gateway-token-2026}}}")
    private String internalGatewaySecret;

    public boolean isTrustedInternalCall(HttpServletRequest req) {
        if (req == null) return false;
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

    public Long getCurrentStudentId(HttpServletRequest req) {
        if (!isTrustedInternalCall(req)) return null;
        return parseLong(req.getHeader("X-Student-Id"));
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

    private Long parseLong(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
