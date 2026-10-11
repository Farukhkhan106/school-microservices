package com.successacademy.authservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardWidgetDto {
    private String key;
    private String title;
    private String description;
    private String componentKey;
    private String icon;
    private Integer displayOrder;
    private Integer gridWidth; // 1 to 12
    private String requiredFeature;
    private String requiredPermission;
    private String requiredScope;
    private boolean enabled;
    private String dataSource;
    private Map<String, Object> data;
}
