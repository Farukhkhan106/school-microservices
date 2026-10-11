package com.successacademy.authservice.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleDto {
    private Long id;
    private Long userId;
    private String username;
    private Long roleId;
    private String roleCode;
    private String roleDisplayName;
    private String tenantId;
    private boolean isPrimary;
    private LocalDateTime assignedAt;
    private String assignedBy;
}
