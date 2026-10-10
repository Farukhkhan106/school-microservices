package com.successacademy.academicservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionTransitionRequest {
    @NotNull(message = "Source session ID is required")
    private Long sourceSessionId;

    @NotNull(message = "Target session ID is required")
    private Long targetSessionId;

    @Builder.Default
    private boolean carryForwardFees = true;

    private String notes;
}
