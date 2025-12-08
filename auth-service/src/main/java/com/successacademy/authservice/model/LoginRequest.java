package com.successacademy.authservice.model;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
