package com.successacademy.facultyservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultySalaryPaymentResponse {

    private Long id;
    private Long facultyId;
    private Long monthlySalaryId;
    private String receiptNumber;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMode;
    private String referenceNumber;
    private String remarks;
    private Long recordedBy;
    private LocalDateTime createdAt;
}
