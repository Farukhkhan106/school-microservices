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
public class UpdateVersionConfigRequest {

    private String changeSummary;
    private Long optLockVersion;
    // Map of category -> config object / JSON
    private Map<String, Object> categories;
}
