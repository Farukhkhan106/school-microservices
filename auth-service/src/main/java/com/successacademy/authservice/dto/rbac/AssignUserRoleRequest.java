package com.successacademy.authservice.dto.rbac;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignUserRoleRequest {
    @NotNull(message = "roleId is required")
    private Long roleId;

    private boolean isPrimary;
}
