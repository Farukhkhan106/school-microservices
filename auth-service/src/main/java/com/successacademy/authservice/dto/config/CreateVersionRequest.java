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
public class CreateVersionRequest {

    private String tenantKey;
    private String environmentKey; // Nullable
    private String changeSummary;
    // Optional initial configuration category payload map: category -> json string or object
    private Map<String, Object> categories;
}
