package com.successacademy.facultyservice.security;

import com.successacademy.facultyservice.exception.ForbiddenException;
import com.successacademy.facultyservice.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Reads the authenticated identity that the API gateway validated and forwarded
 * (X-User-Id / X-User-Role headers). Services must NEVER trust a teacherId or
 * role sent in the request body/params for authorization decisions.
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

    public static Long teacherId(HttpServletRequest request) {
        String v = request.getHeader("X-Teacher-Id");
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

    /** Authenticated request required (any role). */
    public static void requireAuthenticated(HttpServletRequest request) {
        if (userId(request) == null || role(request).isBlank()) {
            throw new UnauthorizedException("Authentication required");
        }
    }

    /** Only one of the given roles may proceed. */
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

    public static boolean isTeacher(HttpServletRequest request) {
        return "TEACHER".equalsIgnoreCase(role(request));
    }

    public static List<String> adminOnly() {
        return List.of("ADMIN");
    }
}