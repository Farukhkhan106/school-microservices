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
public class StaffSalaryConfigResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private String category;
    private String designation;
    private String salaryType;
    private BigDecimal baseSalary;
    private BigDecimal transportAllowance;
    private BigDecimal otherAllowance;
    private BigDecimal fixedDeduction;
    private BigDecimal grossSalary;
    private BigDecimal netSalary;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean active;
    private String remarks;
    private LocalDateTime updatedAt;
}
