package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.ActivityStatus;
import com.successacademy.academicservice.model.ActivityType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityUpdateRequest {

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private ActivityType activityType;

    private LocalDate assignedDate;

    private LocalDate dueDate;

    @DecimalMin(value = "0.01", message = "Max marks must be greater than 0")
    private BigDecimal maxMarks;

    @DecimalMin(value = "0.00", message = "Passing marks cannot be negative")
    private BigDecimal passingMarks;

    private ActivityStatus status;

    @Size(max = 2000, message = "Instructions cannot exceed 2000 characters")
    private String instructions;
}
