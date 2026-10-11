package com.successacademy.authservice.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    @NotBlank(message = "Tenant key is required")
    @Pattern(regexp = "^[a-z0-9_-]{2,64}$", message = "Tenant key must be 2-64 lowercase alphanumeric, dash, or underscore characters")
    private String tenantKey;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private String legalName;
    private String domain;
    private String contactEmail;
    private String contactPhone;
    private String status; // ACTIVE, PENDING, etc.
}
