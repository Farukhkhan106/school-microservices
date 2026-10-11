package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureEntitlementDto {

    private String tenantKey;
    private String environmentKey;
    private String featureKey;
    private boolean enabled;
    private String description;
}
