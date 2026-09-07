package com.successacademy.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private Long id;
    private String username;
    private String email;
    private String role;       // ADMIN / STUDENT / TEACHER
    private String token;      // JWT

    // Links to role-specific profile
    private Long studentId;    // non-null if role = STUDENT
    private Long teacherId;    // non-null if role = TEACHER
}
