package com.successacademy.feeservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class FeePlanDTOs {

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class FeePlanItemDTO {
        private Long id;
        private String feeHead;
        private BigDecimal standardAmount;
        private BigDecimal applicableAmount;
        private Boolean mandatory;
        private Boolean enabled;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class FeeAdjustmentDTO {
        private Long id;
        private String adjustmentType;
        private BigDecimal amount;
        private String reason;
        private LocalDateTime createdAt;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class FeePlanAuditDTO {
        private Long id;
        private BigDecimal previousAmount;
        private BigDecimal newAmount;
        private String changeReason;
        private Long changedBy;
        private LocalDateTime changedAt;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class StudentFeePlanRequest {
        private Long studentId;
        private String studentName;
        private String studentClass;
        private String section;
        private String academicYear; // Defaults to current session if null
        private LocalDate dueDate;
        private String remarks;
        private String feePlanStatus; // "CONFIGURED" | "SETUP_REQUIRED"
        private String changeReason; // Required when updating existing plan
        private List<FeePlanItemDTO> items;
        private List<FeeAdjustmentDTO> adjustments;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class StudentFeePlanResponse {
        private Long id;
        private Long studentId;
        private String studentName;
        private String studentClass;
        private String section;
        private String academicYear;
        private BigDecimal standardFeeAmount;
        private BigDecimal applicableAmount;
        private BigDecimal totalAdjustmentAmount;
        private BigDecimal finalPayableAmount;
        private BigDecimal paidAmount;
        private BigDecimal outstandingBalance;
        private String feePlanStatus;   // SETUP_REQUIRED | CONFIGURED | ACTIVE | CANCELLED
        private String paymentStatus;   // UNPAID | PARTIAL | PAID | OVERDUE
        private LocalDate dueDate;
        private String remarks;
        private Long feeRecordId;
        private List<FeePlanItemDTO> items;
        private List<FeeAdjustmentDTO> adjustments;
        private List<FeePlanAuditDTO> audits;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class ClassFeeTemplateResponse {
        private String className;
        private String academicYear;
        private BigDecimal standardTotal;
        private List<FeePlanItemDTO> items;
    }

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class StudentFeeSummaryDTO {
        private Long studentId;
        private String studentName;
        private String studentClass;
        private String section;
        private String academicYear;
        private String feePlanStatus;   // SETUP_REQUIRED | CONFIGURED | ACTIVE
        private String paymentStatus;   // UNPAID | PARTIAL | PAID | OVERDUE
        private BigDecimal standardFeeAmount;
        private BigDecimal totalAdjustmentAmount;
        private BigDecimal finalPayableAmount;
        private BigDecimal paidAmount;
        private BigDecimal outstandingBalance;
        private Long planId;
        private Long feeRecordId;
    }
}
