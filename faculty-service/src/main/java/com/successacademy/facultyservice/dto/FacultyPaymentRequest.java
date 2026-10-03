package com.successacademy.facultyservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyPaymentRequest {

    private BigDecimal amount;
    private String paymentMode; // CASH, BANK_TRANSFER, CHEQUE, UPI, OTHER
    private String referenceNumber;
    private String remarks;
}
