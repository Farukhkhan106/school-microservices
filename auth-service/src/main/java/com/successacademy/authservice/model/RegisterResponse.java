package com.successacademy.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Long studentId;
    private Long teacherId;
    private String message;
}
