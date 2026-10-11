package com.successacademy.authservice.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectiveDashboardResponse {
    private String tenantKey;
    private String schoolName;
    private String logoUrl;
    private String environment;
    private String username;
    private String userRole;
    private String activeAcademicSession;
    private List<NavigationItemDto> navigation;
    private List<DashboardWidgetDto> widgets;
    private List<QuickActionDto> quickActions;
    private Map<String, Integer> badges;
}
