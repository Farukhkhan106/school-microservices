package com.successacademy.authservice.controller;

import com.successacademy.authservice.model.*;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.service.AuthService;
import jakarta.persistence.*;
import jakarta.validation.Valid;
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
                .map(u -> ResponseEntity.ok(new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStudentId(), u.getTeacherId(), u.getStaffId())))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Get user summary by id (internal / communication) ─────────
    @GetMapping("/user-by-id/{id}")
    public ResponseEntity<UserSummary> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.getStudentId(), u.getTeacherId(), u.getStaffId())))
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

    // ── Update teacher user status (Active / Inactive) ──────────
    @PutMapping("/teacher/{teacherId}/status")
    public ResponseEntity<Void> updateTeacherStatus(
            @PathVariable Long teacherId,
            @RequestParam String status) {
        authService.setTeacherStatus(teacherId, status);
        return ResponseEntity.ok().build();
    }

    private final com.successacademy.authservice.service.user.UserService userService;

    // ── Public Invitation Acceptance ──────────────────────────────
    @PostMapping("/invitation/accept")
    public ResponseEntity<Map<String, String>> acceptInvitation(@Valid @RequestBody com.successacademy.authservice.dto.user.AcceptInvitationRequest request) {
        userService.acceptInvitation(request);
        return ResponseEntity.ok(Map.of("message", "Invitation accepted and account activated successfully."));
    }

    // ── Public Password Reset Request ─────────────────────────────
    @PostMapping("/password/reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody com.successacademy.authservice.dto.user.PasswordResetRequest body,
            jakarta.servlet.http.HttpServletRequest req) {
        String tenantId = req.getHeader("X-Tenant-ID");
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
        userService.initiatePasswordReset(body, tenantId, null, "ANONYMOUS", "PUBLIC");
        return ResponseEntity.ok(Map.of("message", "If an account matching the username or email exists, a password reset link has been processed."));
    }

    // ── Public Password Reset Confirmation ────────────────────────
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<Map<String, String>> confirmPasswordReset(
            @Valid @RequestBody com.successacademy.authservice.dto.user.ConfirmPasswordResetRequest body) {
        userService.confirmPasswordReset(body);
        return ResponseEntity.ok(Map.of("message", "Password has been successfully reset. You may now login."));
    }

    // ── Health check ──────────────────────────────────────────────
    @GetMapping("/test")
    public String test() {
        return "Auth service is working!";
    }
}
