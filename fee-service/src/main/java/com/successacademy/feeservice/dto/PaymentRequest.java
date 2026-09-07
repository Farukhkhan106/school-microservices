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
    private String paymentMethod;   // Cash | Online | Cheque | DD
    private LocalDate paymentDate;
    private String remarks;
}
