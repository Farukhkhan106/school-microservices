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
public class ExportConfigResponse {
    private String schemaVersion;
    private String tenantKey;
    private String environmentKey;
    private Integer versionNumber;
    private String exportedAt;
    private String exportedBy;
    private String checksum;
    private Map<String, Object> categories;
}
