package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_promotions",
    indexes = {
        @Index(name = "idx_promotion_transition_student", columnList = "transition_id, student_id"),
        @Index(name = "idx_promotion_student", columnList = "tenant_id, student_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_promotion_tenant_trans_student", columnNames = {"tenant_id", "transition_id", "student_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPromotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "transition_id", nullable = false)
    private Long transitionId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "admission_no", length = 50)
    private String admissionNo;

    @Column(name = "source_session_id", nullable = false)
    private Long sourceSessionId;

    @Column(name = "target_session_id", nullable = false)
    private Long targetSessionId;

    @Column(name = "source_class", nullable = false, length = 20)
    private String sourceClass;

    @Column(name = "source_section", nullable = false, length = 10)
    private String sourceSection;

    @Column(name = "target_class", length = 20)
    private String targetClass;

    @Column(name = "target_section", length = 10)
    private String targetSection;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "suggested_decision", nullable = false, length = 30)
    @Builder.Default
    private PromotionDecision suggestedDecision = PromotionDecision.PROMOTE;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "final_decision", nullable = false, length = 30)
    @Builder.Default
    private PromotionDecision finalDecision = PromotionDecision.PROMOTE;

    @Column(name = "outstanding_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal outstandingFee = BigDecimal.ZERO;

    @Column(name = "carry_forward_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal carryForwardFee = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, EXECUTED, REJECTED

    @Column(length = 500)
    private String reason;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null || this.tenantId.isBlank()) {
            this.tenantId = "default";
        }
        if (this.suggestedDecision == null) {
            this.suggestedDecision = PromotionDecision.PROMOTE;
        }
        if (this.finalDecision == null) {
            this.finalDecision = this.suggestedDecision;
        }
        if (this.outstandingFee == null) {
            this.outstandingFee = BigDecimal.ZERO;
        }
        if (this.carryForwardFee == null) {
            this.carryForwardFee = BigDecimal.ZERO;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
