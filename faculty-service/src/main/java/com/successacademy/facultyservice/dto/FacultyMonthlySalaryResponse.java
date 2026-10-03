package com.successacademy.facultyservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyMonthlySalaryResponse {

    private Long id;
    private Long facultyId;
    private String facultyName;
    private String facultyCode;
    private String department;
    private String designation;
    private String phone;

    private Integer month;
    private Integer year;

    private Integer workingDays;
    private Double presentDays;
    private Double halfDays;
    private Double absentDays;
    private Double paidLeaveDays;
    private Double unpaidLeaveDays;

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

    private String payrollStatus; // CALCULATED, APPROVED, LOCKED
    private Long approvedBy;
    private LocalDateTime approvedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
