package com.successacademy.authservice.service;

import com.successacademy.authservice.model.LoginRequest;
import com.successacademy.authservice.model.LoginResponse;
import com.successacademy.authservice.model.RegisterRequest;
import com.successacademy.authservice.model.RegisterResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    RegisterResponse register(RegisterRequest request);

    boolean usernameExists(String username);

    void setTeacherStatus(Long teacherId, String status);
}
