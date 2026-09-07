package com.successacademy.attendanceservice.dto;

import lombok.*;

import java.util.Map;

/**
 * Per-student attendance summary used by StudentAcademics page.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AttendanceSummaryResponse {

    private Long studentId;
    private long totalDays;
    private long presentDays;
    private long absentDays;
    private long lateDays;
    private double overallPercentage;

    // subject → attendance % (for per-subject display)
    private Map<String, Double> subjectWisePercentage;
}
