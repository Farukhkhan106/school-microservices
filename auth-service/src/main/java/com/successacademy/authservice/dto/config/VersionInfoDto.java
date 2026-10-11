package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionInfoDto {

    private Long id;
    private String tenantKey;
    private String environmentKey;
    private Integer versionNumber;
    private String status;
    private String changeSummary;
    private Long parentVersionId;
    private Integer rollbackFromVersion;
    private String checksum;
    private Long optLockVersion;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String publishedBy;
    private LocalDateTime publishedAt;
}
