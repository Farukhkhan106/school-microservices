package com.successacademy.authservice.dto.rbac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveAuthorizationResponse {
    private Long userId;
    private String username;
    private String tenantId;
    private List<String> roles;
    private List<String> permissions;
    private Map<String, String> permissionScopes; // permissionKey -> scope (e.g. "student.view" -> "ASSIGNED")
}
