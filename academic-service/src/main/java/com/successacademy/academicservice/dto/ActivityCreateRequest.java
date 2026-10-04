package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityCreateRequest {

    @NotBlank(message = "Activity title is required")
    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Activity type is required")
    private ActivityType activityType;

    @NotBlank(message = "Class is required")
    @Size(max = 20, message = "Class cannot exceed 20 characters")
    private String studentClass;

    @NotBlank(message = "Section is required")
    @Size(max = 10, message = "Section cannot exceed 10 characters")
    private String section;

    @NotBlank(message = "Subject is required")
    @Size(max = 100, message = "Subject cannot exceed 100 characters")
    private String subject;

    @NotNull(message = "Assigned date is required")
    private LocalDate assignedDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Max marks is required")
    @DecimalMin(value = "0.01", message = "Max marks must be greater than 0")
    private BigDecimal maxMarks;

    @DecimalMin(value = "0.00", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    private ActivityStatus status; // Optional: DRAFT or PUBLISHED, defaults to DRAFT

    @Size(max = 2000, message = "Instructions cannot exceed 2000 characters")
    private String instructions;
}
