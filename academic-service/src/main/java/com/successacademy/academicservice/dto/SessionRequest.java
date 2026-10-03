package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRequest {
    @NotBlank(message = "Session code is required (e.g. 2025-2026)")
    private String sessionCode;

    @NotBlank(message = "Session name is required")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private boolean isActive;
    private String description;
}
