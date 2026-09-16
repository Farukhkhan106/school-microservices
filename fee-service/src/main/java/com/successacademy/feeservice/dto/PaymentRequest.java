package com.successacademy.feeservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PaymentRequest {

    private Long feeRecordId;
    private BigDecimal paymentAmount;
    private String paymentMethod;       // Cash | Online | Cheque | DD | UPI
    private String transactionReference;// Optional UTR / Cheque Number / Reference
    private LocalDate paymentDate;
    private String remarks;
    private Long recordedBy;            // Injected from X-User-Id server-side
}
