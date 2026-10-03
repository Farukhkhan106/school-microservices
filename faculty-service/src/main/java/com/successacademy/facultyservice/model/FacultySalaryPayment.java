package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_salary_payments", indexes = {
        @Index(name = "idx_faculty_sal_pay_salary_id", columnList = "monthly_salary_id"),
        @Index(name = "idx_faculty_sal_pay_faculty_id", columnList = "faculty_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultySalaryPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "monthly_salary_id", nullable = false)
    private Long monthlySalaryId;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 64)
    private String receiptNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    // CASH, BANK_TRANSFER, CHEQUE, UPI, OTHER
    @Column(name = "payment_mode", length = 64)
    private String paymentMode;

    @Column(name = "reference_number", length = 128)
    private String referenceNumber;

    @Column(length = 500)
    private String remarks;

    private Long recordedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
