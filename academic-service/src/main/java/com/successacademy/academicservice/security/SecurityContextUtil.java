package com.successacademy.academicservice.security;

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
        return "ADMIN".equals(getCurrentRole(req));
    }

    public boolean isTeacher(HttpServletRequest req) {
        return "TEACHER".equals(getCurrentRole(req));
    }

    public boolean isStudent(HttpServletRequest req) {
        return "STUDENT".equals(getCurrentRole(req));
    }

    public void requireAuthenticated(HttpServletRequest req) {
        if ("ANONYMOUS".equals(getCurrentRole(req)) || getCurrentUserId(req) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
    }

    public void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required.");
        }
    }

    public void requireTeacher(HttpServletRequest req) {
        if (!isTeacher(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Teacher role required.");
        }
    }

    public void requireStudentOrAdmin(HttpServletRequest req) {
        if (!isStudent(req) && !isAdmin(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Student or administrator role required.");
        }
    }

    public void requireAdminOrTeacher(HttpServletRequest req) {
        if (!isAdmin(req) && !isTeacher(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Authorized faculty or admin required.");
        }
    }

    public void validateTeacherOrAdminForSchedule(HttpServletRequest req, Long assignedTeacherId) {
        if (isAdmin(req)) return;
        Long currentTeacherId = getCurrentTeacherId(req);
        if (currentTeacherId == null || !currentTeacherId.equals(assignedTeacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not authorized to manage marks for this assessment schedule.");
        }
    }

    public void validateStudentOrAdmin(HttpServletRequest req, Long targetStudentId) {
        if (isAdmin(req) || isTeacher(req)) return;
        Long currentStudentId = getCurrentStudentId(req);
        if (currentStudentId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required: Missing student identity.");
        }
        if (targetStudentId == null || !currentStudentId.equals(targetStudentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own academic records.");
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
