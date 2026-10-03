package com.successacademy.feeservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;
    private Long feeRecordId;
    private BigDecimal amountPaid;
    private String paymentMethod;
    private String transactionReference;
    private String receiptNumber;
    private LocalDate paymentDate;
    private String remarks;
    private Long recordedBy;
    private LocalDateTime createdAt;

    // Associated Fee Record summary after payment
    private BigDecimal totalAmount;
    private BigDecimal cumulativePaidAmount;
    private BigDecimal outstandingBalance;
    private String feeRecordStatus; // Paid | Partial | Unpaid
}
