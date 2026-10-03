package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.AssessmentStatus;
import com.successacademy.academicservice.model.AssessmentType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentResponse {
    private Long id;
    private Long sessionId;
    private String sessionCode;
    private String name;
    private AssessmentType assessmentType;
    private String assessmentTypeDisplayName;
    private String term;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long gradingSchemeId;
    private String gradingSchemeName;
    private boolean isRankVisible;
    private AssessmentStatus status;
    private String description;
    private int totalSchedulesCount;
    private int submittedSchedulesCount;
    private LocalDateTime createdAt;
}
