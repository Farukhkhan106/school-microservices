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
public class StaffMonthlySalaryResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private String category;
    private String designation;
    private String phone;

    private Integer month;
    private Integer year;

    private Integer workingDays;
    private Double presentDays;
    private Double paidLeaveDays;
    private Double unpaidLeaveDays;
    private Double absentDays;

    private BigDecimal baseSalary;
    private BigDecimal grossSalary;
    private BigDecimal attendanceDeduction;
    private BigDecimal otherDeduction;
    private BigDecimal netSalary;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    private String paymentStatus; // PENDING, PARTIAL, PAID
    private LocalDate paidDate;
    private String paymentMode;
    private String paymentReference;
    private String salaryConfigSnapshot;
    private String remarks;

    private String payrollStatus; // DRAFT, CALCULATED, APPROVED, LOCKED
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Boolean locked;
    private Double unmarkedDays;
    private Boolean prorated;
    private String prorationRemarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
