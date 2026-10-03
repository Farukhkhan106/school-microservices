package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_monthly_salaries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_faculty_month_year", columnNames = {"faculty_id", "salary_month", "salary_year"})
}, indexes = {
        @Index(name = "idx_faculty_month_year", columnList = "salary_month, salary_year"),
        @Index(name = "idx_faculty_salary_faculty_id", columnList = "faculty_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyMonthlySalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "salary_month", nullable = false)
    private Integer month;

    @Column(name = "salary_year", nullable = false)
    private Integer year;

    @Column(nullable = false)
    @Builder.Default
    private Integer workingDays = 26;

    @Builder.Default
    private Double presentDays = 0.0;

    @Builder.Default
    private Double halfDays = 0.0;

    @Builder.Default
    private Double absentDays = 0.0;

    @Builder.Default
    private Double paidLeaveDays = 0.0;

    @Builder.Default
    private Double unpaidLeaveDays = 0.0;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

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

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    // PENDING, PARTIAL, PAID
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String paymentStatus = "PENDING";

    private LocalDate paidDate;

    @Column(length = 64)
    private String paymentMode;

    @Column(length = 128)
    private String paymentReference;

    @Column(columnDefinition = "TEXT")
    private String salaryConfigSnapshot;

    @Column(length = 500)
    private String remarks;

    // CALCULATED, APPROVED, LOCKED
    @Column(length = 32)
    @Builder.Default
    private String payrollStatus = "CALCULATED";

    private Long approvedBy;

    private LocalDateTime approvedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
