package com.successacademy.authservice.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private Long id;
    private String permissionKey;
    private String resourceName;
    private String actionName;
    private String description;
    private String category;
    private boolean isSystem;
    private boolean isProtected;
}
