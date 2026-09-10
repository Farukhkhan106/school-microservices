package com.successacademy.authservice.service;

import com.successacademy.authservice.model.*;
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

    // ── LOGIN ────────────────────────────────────────────────────
    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getId(), user.getStudentId());

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token,
                user.getStudentId(),
                user.getTeacherId()
        );
    }

    // ── REGISTER ─────────────────────────────────────────────────
    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername().toLowerCase().trim());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole().toUpperCase());
        user.setStudentId(request.getStudentId());
        user.setTeacherId(request.getTeacherId());

        User saved = userRepository.save(user);

        return new RegisterResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole(),
                saved.getStudentId(),
                saved.getTeacherId(),
                "User registered successfully"
        );
    }

    // ── CHECK USERNAME ────────────────────────────────────────────
    @Override
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username.toLowerCase()).isPresent();
    }
}
