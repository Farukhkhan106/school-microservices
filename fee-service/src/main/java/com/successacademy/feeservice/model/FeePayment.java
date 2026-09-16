package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * IMMUTABLE PAYMENT TRANSACTION LEDGER:
 * Every payment made against a FeeRecord creates a new, immutable row in this table.
 * Historical records are never overwritten when partial/subsequent payments occur.
 */
@Entity
@Table(
    name = "fee_payments",
    indexes = {
        @Index(name = "idx_fee_record_id", columnList = "fee_record_id"),
        @Index(name = "idx_receipt_number", columnList = "receipt_number", unique = true),
        @Index(name = "idx_transaction_ref", columnList = "transaction_reference")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_record_id", nullable = false)
    private Long feeRecordId;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid;

    // Cash | Online | Cheque | DD | UPI
    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    // Optional reference / UTR / Cheque Number
    @Column(name = "transaction_reference", length = 128)
    private String transactionReference;

    // Server-generated unique receipt number (e.g. REC-2026-00001)
    @Column(name = "receipt_number", nullable = false, unique = true, length = 64)
    private String receiptNumber;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(length = 255)
    private String remarks;

    // Admin user ID who recorded the transaction
    @Column(name = "recorded_by")
    private Long recordedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
