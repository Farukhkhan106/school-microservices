package com.successacademy.studentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLifecycleActionRequest {
    private String action; // TRANSFER, WITHDRAW, GRADUATE, ACTIVATE, DEACTIVATE
    private LocalDate effectiveDate;

    @NotBlank(message = "Reason is required for lifecycle actions")
    private String reason;

    private String notes;
    private String destinationInstitution; // Applicable for TRANSFER
}
