package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.PromotionDecision;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPromotionItemDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String sourceClass;
    private String sourceSection;
    private String targetClass;
    private String targetSection;
    private PromotionDecision suggestedDecision;
    private PromotionDecision finalDecision;
    private BigDecimal outstandingFee;
    private BigDecimal carryForwardFee;
    private String status;
    private String reason;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
}
