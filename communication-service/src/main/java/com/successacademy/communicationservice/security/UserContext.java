package com.successacademy.communicationservice.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserContext {
    private Long userId;
    private String username;
    private String role; // ADMIN, TEACHER, STUDENT
    private Long studentId;
    private Long teacherId;

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isTeacher() {
        return "TEACHER".equalsIgnoreCase(role);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(role);
    }
}
