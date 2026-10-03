package com.successacademy.staffservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalaryPaymentResponse {

    private Long id;
    private Long monthlySalaryId;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMode;
    private String referenceNumber;
    private String receiptNumber;
    private String remarks;
    private Long createdBy;
    private LocalDateTime createdAt;
}
