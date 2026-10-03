package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_salary_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffSalaryPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monthly_salary_id", nullable = false)
    private Long monthlySalaryId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 50)
    private String paymentMode; // CASH, BANK_TRANSFER, UPI, CHEQUE

    @Column(length = 100)
    private String referenceNumber; // Transaction ID, Cheque #, etc.

    @Column(length = 50)
    private String receiptNumber; // e.g. SAL-2026-0001

    @Column(length = 300)
    private String remarks;

    private Long createdBy; // actorUserId

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.paymentDate == null) this.paymentDate = LocalDate.now();
    }
}
