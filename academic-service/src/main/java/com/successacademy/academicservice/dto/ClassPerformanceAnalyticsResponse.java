package com.successacademy.academicservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassPerformanceAnalyticsResponse {
    private Long assessmentId;
    private String assessmentName;
    private String studentClass;
    private String section;
    private int totalStrength;
    private int appearedStudents;
    private int absentStudents;
    private int passedStudents;
    private int failedStudents;
    private BigDecimal passPercentage;
    private BigDecimal classAveragePercentage;
    private BigDecimal highestPercentage;
    private String classTopperName;
    private BigDecimal lowestPercentage;
    private List<SubjectPerformanceDto> subjects;
    private List<StudentRankDto> rankings;
    private List<StudentRankDto> remedialStudents; // Students needing attention (e.g. <33% or failing a subject)
}
