package com.successacademy.authservice.controller;

import com.successacademy.authservice.model.*;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Login ─────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ── Register new user (called by student-service / faculty-service internally) ──
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // ── Check if username is taken ────────────────────────────────
    @GetMapping("/exists/{username}")
    public ResponseEntity<Boolean> exists(@PathVariable String username) {
        return ResponseEntity.ok(authService.usernameExists(username));
    }

    // ── Get user summary by username (internal / communication) ───
    @GetMapping("/user/{username}")
    public ResponseEntity<UserSummary> getUserByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username.toLowerCase().trim())
                .map(u -> ResponseEntity.ok(new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStudentId(), u.getTeacherId())))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Get user summary by id (internal / communication) ─────────
    @GetMapping("/user-by-id/{id}")
    public ResponseEntity<UserSummary> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStudentId(), u.getTeacherId())))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Change password ───────────────────────────────────────────
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.status(401).body("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok("Password changed successfully");
    }

    // ── Health check ──────────────────────────────────────────────
    @GetMapping("/test")
    public String test() {
        return "Auth service is working!";
    }
}
