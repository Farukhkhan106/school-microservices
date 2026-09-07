package com.successacademy.authservice.model;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;   // e.g. "rahul.sharma"
    private String password;   // plain text — will be BCrypt encoded
    private String email;
    private String role;       // STUDENT | TEACHER | ADMIN
    private Long studentId;    // nullable
    private Long teacherId;    // nullable
}
