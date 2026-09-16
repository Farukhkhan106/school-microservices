package com.successacademy.chatbotservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

public final class UserContext {

    private UserContext() {}

    public static Long getUserId(HttpServletRequest request) {
        String val = request.getHeader("X-User-Id");
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getRole(HttpServletRequest request) {
        String val = request.getHeader("X-User-Role");
        return val != null ? val.trim().toUpperCase() : null;
    }

    public static Long getStudentId(HttpServletRequest request) {
        String val = request.getHeader("X-Student-Id");
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long getTeacherId(HttpServletRequest request) {
        String val = request.getHeader("X-Teacher-Id");
        if (val == null || val.isBlank()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void requireRole(HttpServletRequest request, String... allowedRoles) {
        String role = getRole(request);
        if (role == null || Arrays.stream(allowedRoles).noneMatch(role::equalsIgnoreCase)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Required role not present");
        }
    }

    public static void requireAuthenticated(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required: Missing or invalid token");
        }
    }

    public static void requireConversationOwnership(HttpServletRequest request, Long resourceUserId) {
        Long callerUserId = getUserId(request);
        if (callerUserId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (resourceUserId == null || !callerUserId.equals(resourceUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You cannot access or modify this conversation");
        }
    }
}
