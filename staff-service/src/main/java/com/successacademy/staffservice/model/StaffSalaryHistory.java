package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_salary_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal allowances;

    @Column(precision = 12, scale = 2)
    private BigDecimal otherFixedEarnings;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Column(length = 300)
    private String reason;

    private Long createdByUserId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.effectiveFrom == null) this.effectiveFrom = LocalDate.now();
        if (this.monthlySalary == null) this.monthlySalary = BigDecimal.ZERO;
    }
}
