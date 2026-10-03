package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fee_adjustments",
    indexes = {
        @Index(name = "idx_adjustment_plan_id", columnList = "fee_plan_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeeAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_plan_id", nullable = false)
    private Long feePlanId;

    @Column(name = "student_id")
    private Long studentId;

    // Sibling Discount | Parent Negotiation | Scholarship | Financial Concession | Management Concession | Staff Concession | Special Discount | Other
    @Column(name = "adjustment_type", nullable = false, length = 64)
    private String adjustmentType;

    @Column(name = "type")
    private String type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.adjustmentType != null) {
            this.type = this.adjustmentType;
        }
    }
}
