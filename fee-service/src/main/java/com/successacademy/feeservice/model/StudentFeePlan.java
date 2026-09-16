package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_fee_plans",
    indexes = {
        @Index(name = "idx_student_academic_year", columnList = "student_id, academic_year", unique = true),
        @Index(name = "idx_plan_status", columnList = "fee_plan_status"),
        @Index(name = "idx_student_class", columnList = "student_class")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StudentFeePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    // Historical snapshots of student metadata at time of fee plan creation
    @Column(name = "student_name")
    private String studentName;

    @Column(name = "student_class")
    private String studentClass;     // e.g. "10" or "10-A"

    @Column(name = "section")
    private String section;          // e.g. "A"

    @Column(name = "academic_year", nullable = false, length = 32)
    private String academicYear;     // e.g. "2026-27"

    @Column(name = "standard_fee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardFeeAmount; // Total of standard template (e.g. ₹42,000)

    @Column(name = "applicable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal applicableAmount;  // Sum of enabled components (e.g. ₹32,000)

    @Column(name = "total_adjustment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAdjustmentAmount; // Total concessions/discounts (e.g. ₹2,000)

    @Column(name = "final_payable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPayableAmount; // Actual amount student owes (e.g. ₹30,000)

    // Separate FeePlanStatus: SETUP_REQUIRED | CONFIGURED | ACTIVE | CANCELLED
    @Column(name = "fee_plan_status", nullable = false, length = 32)
    private String feePlanStatus;

    // Status alias for database compatibility
    @Column(name = "status")
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 500)
    private String remarks;

    @Column(name = "fee_record_id")
    private Long feeRecordId;        // Associated FeeRecord (Account Receivable)

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.feePlanStatus == null) {
            this.feePlanStatus = "SETUP_REQUIRED";
        }
        this.status = this.feePlanStatus;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.feePlanStatus != null) {
            this.status = this.feePlanStatus;
        }
    }
}
