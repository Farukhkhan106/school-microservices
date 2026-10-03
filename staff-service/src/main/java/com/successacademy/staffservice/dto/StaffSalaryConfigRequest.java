package com.successacademy.staffservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalaryConfigRequest {

    private String salaryType; // MONTHLY, DAILY
    private BigDecimal baseSalary;
    private BigDecimal transportAllowance;
    private BigDecimal otherAllowance;
    private BigDecimal fixedDeduction;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String remarks;
}
