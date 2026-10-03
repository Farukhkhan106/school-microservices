package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.AssessmentComponent;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleCreateRequest {
    @NotNull(message = "Assessment ID is required")
    private Long assessmentId;

    @NotBlank(message = "Class name is required")
    private String studentClass; // e.g. "10", "9"

    @NotBlank(message = "Section is required")
    private String section; // e.g. "A"

    @NotBlank(message = "Subject is required")
    private String subject; // e.g. "Mathematics"

    private AssessmentComponent component; // Defaults to THEORY

    @NotNull(message = "Exam date is required")
    private LocalDate examDate;

    private Integer periodNo; // 1 to 8, or null for term exam

    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    private String teacherName;

    @NotNull(message = "Max marks is required")
    @DecimalMin(value = "1.0", message = "Max marks must be greater than 0")
    private BigDecimal maxMarks;

    @NotNull(message = "Pass marks is required")
    @DecimalMin(value = "0.0", message = "Pass marks cannot be negative")
    private BigDecimal passMarks;
}
