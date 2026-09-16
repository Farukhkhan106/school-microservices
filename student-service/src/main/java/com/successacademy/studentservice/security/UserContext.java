package com.successacademy.studentservice.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserContext {
    private Long userId;
    private String role;
    private Long studentId;
    private Long teacherId;

    public static UserContext fromRequest(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        String roleStr = request.getHeader("X-User-Role");
        String studentIdStr = request.getHeader("X-Student-Id");
        String teacherIdStr = request.getHeader("X-Teacher-Id");

        return UserContext.builder()
                .userId(parseId(userIdStr))
                .role(roleStr != null ? roleStr.trim().toUpperCase() : null)
                .studentId(parseId(studentIdStr))
                .teacherId(parseId(teacherIdStr))
                .build();
    }

    private static Long parseId(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            long parsed = Long.parseLong(val.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
