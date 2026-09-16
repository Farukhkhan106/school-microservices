package com.successacademy.feeservice.security;

import com.successacademy.feeservice.exception.ForbiddenException;
import com.successacademy.feeservice.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves authenticated identity and roles from headers injected by the API Gateway
 * (X-User-Id, X-User-Role, X-Student-Id). Services must NEVER trust client-supplied IDs.
 */
public final class UserContext {

    private UserContext() {}

    public static Long userId(HttpServletRequest request) {
        String v = request.getHeader("X-User-Id");
        if (v == null || v.isBlank() || "null".equals(v)) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String role(HttpServletRequest request) {
        String r = request.getHeader("X-User-Role");
        return r == null ? "" : r.trim();
    }

    public static Long studentId(HttpServletRequest request) {
        String s = request.getHeader("X-Student-Id");
        if (s == null || s.isBlank() || "null".equals(s)) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Ensure request has valid credentials */
    public static void requireAuthenticated(HttpServletRequest request) {
        if (userId(request) == null || role(request).isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    /** Require user to have one of the specified roles */
    public static void requireRole(HttpServletRequest request, String... allowed) {
        requireAuthenticated(request);
        String r = role(request);
        for (String a : allowed) {
            if (a.equalsIgnoreCase(r)) return;
        }
        throw new ForbiddenException("You do not have permission to perform this action");
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equalsIgnoreCase(role(request));
    }

    public static boolean isStudent(HttpServletRequest request) {
        return "STUDENT".equalsIgnoreCase(role(request));
    }

    /** Verify that a student can only view/access their own records */
    public static void requireStudentOwnershipOrAdmin(HttpServletRequest request, Long targetStudentId) {
        requireAuthenticated(request);
        if (isAdmin(request)) return;
        if (isStudent(request)) {
            Long myStudentId = studentId(request);
            if (myStudentId != null && myStudentId.equals(targetStudentId)) {
                return;
            }
        }
        throw new ForbiddenException("You are not authorized to view financial records for this student");
    }
}
