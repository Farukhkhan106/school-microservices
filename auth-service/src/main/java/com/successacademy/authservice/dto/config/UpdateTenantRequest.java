package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {

    private String displayName;
    private String legalName;
    private String status; // ACTIVE, SUSPENDED, INACTIVE
    private String domain;
    private String contactEmail;
    private String contactPhone;
}
