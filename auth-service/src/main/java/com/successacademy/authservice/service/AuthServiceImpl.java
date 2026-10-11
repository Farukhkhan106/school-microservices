package com.successacademy.authservice.service;

import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.model.*;
import com.successacademy.authservice.model.entity.Role;
import com.successacademy.authservice.model.entity.UserRole;
import com.successacademy.authservice.repository.RoleRepository;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.repository.UserRoleRepository;
import com.successacademy.authservice.security.JwtUtil;
import com.successacademy.authservice.service.rbac.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RbacService rbacService;

    // ── LOGIN ────────────────────────────────────────────────────
    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Phase 6 Account Lifecycle Enforcement
        if (user.getStatus() != null && !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn("Login rejected for user {}: status is {}", user.getUsername(), user.getStatus());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Account login rejected. Account status is " + user.getStatus() + ".");
        }

        // Record successful login timestamp
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String tenantId = (user.getTenantId() != null && !user.getTenantId().isBlank()) ? user.getTenantId() : "default";

        String token;
        if (user.getGuardianId() != null) {
            token = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getRole(),
                    user.getId(),
                    user.getStudentId(),
                    user.getTeacherId(),
                    user.getStaffId(),
                    user.getGuardianId(),
                    tenantId
            );
        } else {
            token = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getRole(),
                    user.getId(),
                    user.getStudentId(),
                    user.getTeacherId(),
                    user.getStaffId(),
                    tenantId
            );
        }

        List<String> roles = Collections.emptyList();
        List<String> permissions = Collections.emptyList();
        Map<String, String> scopes = Collections.emptyMap();

        try {
            EffectiveAuthorizationResponse authz = rbacService.getEffectiveAuthorization(user.getId(), tenantId);
            if (authz != null) {
                roles = authz.getRoles();
                permissions = authz.getPermissions();
                scopes = authz.getPermissionScopes();
            }
        } catch (Exception ex) {
            log.warn("Could not resolve effective permissions for user {}: {}", user.getUsername(), ex.getMessage());
        }

        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .token(token)
                .studentId(user.getStudentId())
                .teacherId(user.getTeacherId())
                .staffId(user.getStaffId())
                .tenantId(tenantId)
                .roles(roles)
                .permissions(permissions)
                .permissionScopes(scopes)
                .build();
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
        user.setStaffId(request.getStaffId());
        user.setTenantId("default");

        User saved = userRepository.save(user);

        // Bind role in user_roles table
        try {
            String roleCode = saved.getRole();
            if ("ADMIN".equals(roleCode)) roleCode = "TENANT_ADMIN";
            final String lookup = roleCode;
            Role role = roleRepository.findByRoleCodeAndTenantIdIsNull(lookup)
                    .or(() -> roleRepository.findByRoleCodeAndTenantId(lookup, "default"))
                    .orElse(null);

            if (role != null) {
                UserRole ur = UserRole.builder()
                        .user(saved)
                        .role(role)
                        .tenantId("default")
                        .isPrimary(true)
                        .assignedBy("SYSTEM_REGISTRATION")
                        .build();
                userRoleRepository.save(ur);
            }
        } catch (Exception ex) {
            log.warn("Could not bind user_roles during registration for {}: {}", saved.getUsername(), ex.getMessage());
        }

        return new RegisterResponse(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getRole(),
                saved.getStudentId(),
                saved.getTeacherId(),
                saved.getStaffId(),
                "User registered successfully"
        );
    }

    // ── CHECK USERNAME ────────────────────────────────────────────
    @Override
    public boolean usernameExists(String username) {
        return userRepository.findByUsername(username.toLowerCase()).isPresent();
    }

    // ── UPDATE TEACHER STATUS ──────────────────────────────────────
    @Override
    public void setTeacherStatus(Long teacherId, String status) {
        if (teacherId == null) return;
        userRepository.findByTeacherId(teacherId).ifPresent(user -> {
            user.setStatus(status != null ? status.trim().toUpperCase() : "ACTIVE");
            userRepository.save(user);
        });
    }
}
