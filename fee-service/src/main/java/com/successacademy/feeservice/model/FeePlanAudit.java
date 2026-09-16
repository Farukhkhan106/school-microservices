package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fee_plan_audits",
    indexes = {
        @Index(name = "idx_audit_plan_id", columnList = "fee_plan_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeePlanAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_plan_id", nullable = false)
    private Long feePlanId;

    @Column(name = "previous_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal previousAmount;

    @Column(name = "new_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "change_reason", nullable = false, length = 500)
    private String changeReason;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
