package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFeatureEntitlementsRequest {

    private String tenantKey;
    private String environmentKey;
    // Map of featureKey -> enabled boolean
    private Map<String, Boolean> features;
}
