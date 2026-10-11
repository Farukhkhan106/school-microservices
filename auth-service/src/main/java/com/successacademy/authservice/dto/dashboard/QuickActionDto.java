package com.successacademy.authservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickActionDto {
    private String key;
    private String label;
    private String icon;
    private String route;
    private String requiredFeature;
    private String requiredPermission;
    private String requiredScope;
    private String color;
}
