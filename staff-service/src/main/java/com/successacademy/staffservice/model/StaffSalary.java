package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_salaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false, unique = true)
    private Long staffId;

    // Salary Type: MONTHLY, DAILY
    @Column(length = 20)
    @Builder.Default
    private String salaryType = "MONTHLY";

    // Monthly Salary (e.g. 18000.00)
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal monthlySalary = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal baseSalary = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal transportAllowance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fixedDeduction = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal allowances;

    @Column(precision = 12, scale = 2)
    private BigDecimal otherFixedEarnings;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate effectiveFrom = LocalDate.now();

    private LocalDate effectiveTo;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 200)
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.salaryType == null) this.salaryType = "MONTHLY";
        if (this.baseSalary == null) {
            this.baseSalary = (this.basicSalary != null) ? this.basicSalary : (this.monthlySalary != null ? this.monthlySalary : BigDecimal.ZERO);
        }
        if (this.monthlySalary == null || this.monthlySalary.compareTo(BigDecimal.ZERO) == 0) {
            this.monthlySalary = this.baseSalary;
        }
        if (this.transportAllowance == null) this.transportAllowance = BigDecimal.ZERO;
        if (this.otherAllowance == null) this.otherAllowance = BigDecimal.ZERO;
        if (this.fixedDeduction == null) this.fixedDeduction = BigDecimal.ZERO;
        if (this.effectiveFrom == null) this.effectiveFrom = LocalDate.now();
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
