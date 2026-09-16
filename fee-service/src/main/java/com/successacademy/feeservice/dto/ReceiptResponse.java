package com.successacademy.feeservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReceiptResponse {

    private String receiptNumber;
    private Long paymentId;
    private Long feeRecordId;
    private Long studentId;
    private String studentName;
    private String studentClass;
    private String feeType;
    private BigDecimal totalFee;
    private BigDecimal amountPaidInThisTx;
    private BigDecimal cumulativePaidAmount;
    private BigDecimal outstandingAmount;
    private String paymentMethod;
    private String transactionReference;
    private LocalDate paymentDate;
    private String remarks;
    private Long recordedBy;
    private String status;
    private LocalDateTime generatedAt;
}
