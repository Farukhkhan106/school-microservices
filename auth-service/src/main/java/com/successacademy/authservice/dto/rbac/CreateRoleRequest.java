package com.successacademy.authservice.dto.rbac;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {
    @NotBlank(message = "roleCode is required")
    private String roleCode;

    @NotBlank(message = "displayName is required")
    private String displayName;

    private String description;

    private List<RolePermissionAssignmentDto> permissions;
}
