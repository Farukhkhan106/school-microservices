package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_session_transitions",
    indexes = {
        @Index(name = "idx_transition_tenant_status", columnList = "tenant_id, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSessionTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "source_session_id", nullable = false)
    private Long sourceSessionId;

    @Column(name = "source_session_code", nullable = false, length = 50)
    private String sourceSessionCode;

    @Column(name = "target_session_id", nullable = false)
    private Long targetSessionId;

    @Column(name = "target_session_code", nullable = false, length = 50)
    private String targetSessionCode;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransitionStatus status = TransitionStatus.DRAFT;

    @Column(name = "total_students", nullable = false)
    @Builder.Default
    private int totalStudents = 0;

    @Column(name = "promoted_count", nullable = false)
    @Builder.Default
    private int promotedCount = 0;

    @Column(name = "retained_count", nullable = false)
    @Builder.Default
    private int retainedCount = 0;

    @Column(name = "graduated_count", nullable = false)
    @Builder.Default
    private int graduatedCount = 0;

    @Column(name = "transferred_count", nullable = false)
    @Builder.Default
    private int transferredCount = 0;

    @Column(name = "withdrawn_count", nullable = false)
    @Builder.Default
    private int withdrawnCount = 0;

    @Column(name = "total_carry_forward_fees", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCarryForwardFees = BigDecimal.ZERO;

    @Column(name = "fee_arrears_students_count", nullable = false)
    @Builder.Default
    private int feeArrearsStudentsCount = 0;

    @Column(length = 1000)
    private String notes;

    @Column(name = "executed_by")
    private Long executedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null || this.tenantId.isBlank()) {
            this.tenantId = "default";
        }
        if (this.status == null) {
            this.status = TransitionStatus.DRAFT;
        }
        if (this.totalCarryForwardFees == null) {
            this.totalCarryForwardFees = BigDecimal.ZERO;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
