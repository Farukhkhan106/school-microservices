package com.successacademy.authservice.service;

import com.successacademy.authservice.model.LoginRequest;
import com.successacademy.authservice.model.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
