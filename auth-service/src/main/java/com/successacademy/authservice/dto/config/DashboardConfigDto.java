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
public class DashboardConfigDto {
    private boolean showKpiCards;
    private boolean showAttendanceAnalytics;
    private boolean showUpcomingEvents;
    private boolean showRecentNotices;
    private Map<String, Object> customWidgets;
}
