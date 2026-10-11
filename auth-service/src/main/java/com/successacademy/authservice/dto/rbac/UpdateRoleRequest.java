package com.successacademy.authservice.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {
    private String displayName;
    private String description;
    private String status;
    private List<RolePermissionAssignmentDto> permissions;
}
