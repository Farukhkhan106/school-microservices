package com.successacademy.feeservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeeStatsResponse {

    private BigDecimal totalRevenue;    // sum of all paidAmount
    private BigDecimal pendingAmount;   // sum of (amount - paidAmount) for unpaid
    private long totalRecords;
    private long paidCount;
    private long partialCount;
    private long unpaidCount;
    private long overdueCount;
}
