package com.successacademy.authservice.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigDiffItemDto {
    private String category;
    private String path;
    private Object oldValue;
    private Object newValue;
    private String changeType; // ADDED, REMOVED, CHANGED, UNCHANGED
}
