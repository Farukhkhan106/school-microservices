package com.successacademy.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private Long id;
    private String username;
    private String email;
    private String role;       // Primary role: ADMIN / STUDENT / TEACHER / etc. (backward compatible)
    private String token;      // JWT

    // Links to role-specific profile
    private Long studentId;    // non-null if role = STUDENT
    private Long teacherId;    // non-null if role = TEACHER
    private Long staffId;      // non-null if role = STAFF

    private String tenantId;   // Multi-tenant key

    // Phase 3 Granular RBAC additions
    private List<String> roles;
    private List<String> permissions;
    private Map<String, String> permissionScopes;

    // Backward-compatible constructor
    public LoginResponse(Long id, String username, String email, String role, String token,
                         Long studentId, Long teacherId, Long staffId, String tenantId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.token = token;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.staffId = staffId;
        this.tenantId = tenantId;
    }
}
