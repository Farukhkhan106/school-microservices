package com.successacademy.authservice.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionAssignmentDto {
    private String permissionKey;
    private String defaultScope; // OWN, ASSIGNED, ASSIGNED_CLASS, TENANT, ALL
}
