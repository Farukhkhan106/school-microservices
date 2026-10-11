package com.successacademy.authservice.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Long id;
    private String tenantId;
    private String roleCode;
    private String displayName;
    private String description;
    private boolean isSystem;
    private boolean isProtected;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<RolePermissionAssignmentDto> permissions;
}
