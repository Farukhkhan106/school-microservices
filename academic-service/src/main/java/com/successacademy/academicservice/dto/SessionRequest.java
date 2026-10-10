package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionRequest {
    private String tenantId;

    @NotBlank(message = "Session code is required (e.g. 2026-2027)")
    private String sessionCode;

    @NotBlank(message = "Session name is required")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private SessionStatus status;
    private boolean isActive;
    private String description;
}
