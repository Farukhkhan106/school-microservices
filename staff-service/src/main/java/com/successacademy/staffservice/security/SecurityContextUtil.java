package com.successacademy.staffservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SecurityContextUtil {

    public String getCurrentRole(HttpServletRequest req) {
        String role = req.getHeader("X-User-Role");
        if (role == null || role.isBlank()) return "ANONYMOUS";
        String clean = role.trim().toUpperCase();
        if (clean.startsWith("ROLE_")) {
            clean = clean.substring(5);
        }
        return clean;
    }

    public Long getCurrentUserId(HttpServletRequest req) {
        String idStr = req.getHeader("X-User-Id");
        return parseLong(idStr);
    }

    public Long getCurrentStaffId(HttpServletRequest req) {
        String idStr = req.getHeader("X-Staff-Id");
        return parseLong(idStr);
    }

    public String getCurrentUsername(HttpServletRequest req) {
        String uname = req.getHeader("X-Username");
        return uname != null ? uname.trim() : null;
    }

    public boolean isAdmin(HttpServletRequest req) {
        String role = getCurrentRole(req);
        return "ADMIN".equals(role) || "SUPPORT".equals(role);
    }

    public boolean isStaff(HttpServletRequest req) {
        return "STAFF".equals(getCurrentRole(req));
    }

    public void requireAdmin(HttpServletRequest req) {
        if (!isAdmin(req)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator or Support role required.");
        }
    }

    public void requireSelfOrAdmin(HttpServletRequest req, Long targetStaffId) {
        if (isAdmin(req)) return;

        Long currentStaffId = getCurrentStaffId(req);
        if (currentStaffId == null || !currentStaffId.equals(targetStaffId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only access your own staff records.");
        }
    }

    public Long requireStaffIdentity(HttpServletRequest req) {
        Long staffId = getCurrentStaffId(req);
        if (staffId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Staff identity not present in authentication context.");
        }
        return staffId;
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
