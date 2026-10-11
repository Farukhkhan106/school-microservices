package com.successacademy.authservice.dto.user;

import com.successacademy.authservice.dto.rbac.UserRoleDto;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String displayName;
    private String role;
    private String userType;
    private String status;
    private String tenantId;

    // Business entity linking
    private Long studentId;
    private Long teacherId;
    private Long staffId;

    private boolean mustChangePassword;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // RBAC details
    private List<UserRoleDto> roles;
    private List<String> effectivePermissions;
    private Map<String, String> effectiveScopes;
}
