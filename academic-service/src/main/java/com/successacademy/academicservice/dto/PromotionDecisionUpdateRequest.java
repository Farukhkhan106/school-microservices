package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.PromotionDecision;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionDecisionUpdateRequest {
    @NotNull(message = "Final decision is required")
    private PromotionDecision finalDecision;

    private String targetClass;
    private String targetSection;
    private BigDecimal carryForwardFee;
    private String reason;
}
