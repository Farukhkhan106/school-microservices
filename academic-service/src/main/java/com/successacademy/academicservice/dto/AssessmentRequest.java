package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.AssessmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentRequest {
    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotBlank(message = "Assessment name is required")
    private String name;

    @NotNull(message = "Assessment type is required")
    private AssessmentType assessmentType;

    private String term; // e.g. "Term 1", "Term 2"
    private LocalDate startDate;
    private LocalDate endDate;
    private Long gradingSchemeId;
    private Boolean isRankVisible;
    private String description;
}
