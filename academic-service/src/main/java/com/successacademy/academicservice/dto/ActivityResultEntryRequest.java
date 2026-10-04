package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResultEntryRequest {

    @NotNull(message = "Student ID is required")
    private Long studentId;

    @DecimalMin(value = "0.00", message = "Obtained marks cannot be negative")
    private BigDecimal obtainedMarks;

    private Boolean isAbsent;

    @Size(max = 1000, message = "Feedback cannot exceed 1000 characters")
    private String teacherFeedback;
}
