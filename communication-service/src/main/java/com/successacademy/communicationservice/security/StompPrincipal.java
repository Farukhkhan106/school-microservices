package com.successacademy.communicationservice.security;

import lombok.Getter;

import java.security.Principal;

@Getter
public class StompPrincipal implements Principal {

    private final Long userId;
    private final String username;
    private final String role;
    private final Long studentId;
    private final Long teacherId;

    public StompPrincipal(Long userId, String username, String role, Long studentId, Long teacherId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.studentId = studentId;
        this.teacherId = teacherId;
    }

    @Override
    public String getName() {
        return username != null ? username : (userId != null ? userId.toString() : "anonymous");
    }

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
