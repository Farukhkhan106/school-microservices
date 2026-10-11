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
    private Integer versionNumber;
    private String status;
    private String changeSummary;
    private String createdBy;
    private LocalDateTime createdAt;
    private String publishedBy;
    private LocalDateTime publishedAt;
}
