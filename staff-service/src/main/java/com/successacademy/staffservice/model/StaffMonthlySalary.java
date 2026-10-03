package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "staff_monthly_salaries",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_staff_month_year",
        columnNames = {"staff_id", "month", "year"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffMonthlySalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(nullable = false)
    private Integer month; // 1 to 12

    @Column(nullable = false)
    private Integer year; // e.g. 2026

    @Column(nullable = false)
    @Builder.Default
    private Integer workingDays = 26;

    @Column(nullable = false)
    @Builder.Default
    private Double presentDays = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double paidLeaveDays = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double unpaidLeaveDays = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double absentDays = 0.0;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal attendanceDeduction = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal netSalary = BigDecimal.ZERO;

    // Amount actually paid so far (supports partial payments)
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // PENDING, PARTIAL, PAID
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String paymentStatus = "PENDING";

    private LocalDate paidDate;

    @Column(length = 50)
    private String paymentMode; // CASH, BANK_TRANSFER, UPI, CHEQUE

    @Column(length = 100)
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String salaryConfigSnapshot;

    @Column(length = 300)
    private String remarks;

    // Lifecycle status: DRAFT, CALCULATED, APPROVED, LOCKED
    @Column(length = 30)
    @Builder.Default
    private String payrollStatus = "CALCULATED";

    private Long approvedBy; // actorUserId
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean locked = false;

    private Long lockedBy;
    private LocalDateTime lockedAt;

    @Column(length = 300)
    private String unlockReason;

    // Attendance breakdown context
    @Column(nullable = false)
    @Builder.Default
    private Double unmarkedDays = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean prorated = false;

    @Column(length = 300)
    private String prorationRemarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.paymentStatus == null) this.paymentStatus = "PENDING";
        if (this.grossSalary == null) this.grossSalary = BigDecimal.ZERO;
        if (this.attendanceDeduction == null) this.attendanceDeduction = BigDecimal.ZERO;
        if (this.otherDeduction == null) this.otherDeduction = BigDecimal.ZERO;
        if (this.netSalary == null) this.netSalary = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
