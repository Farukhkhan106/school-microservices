package com.successacademy.authservice.service;

import com.successacademy.authservice.model.LoginRequest;
import com.successacademy.authservice.model.LoginResponse;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return new LoginResponse(
                user.getUsername(),
                user.getRole(),
                token
        );
    }
}
