package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.TransitionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitionPreviewResponse {
    private Long id;
    private String tenantId;
    private Long sourceSessionId;
    private String sourceSessionCode;
    private Long targetSessionId;
    private String targetSessionCode;
    private TransitionStatus status;

    private int totalStudents;
    private int promotedCount;
    private int retainedCount;
    private int graduatedCount;
    private int transferredCount;
    private int withdrawnCount;

    private BigDecimal totalCarryForwardFees;
    private int feeArrearsStudentsCount;

    private List<StudentPromotionItemDto> students;

    private String notes;
    private Long executedBy;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
