package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_enrollments",
    indexes = {
        @Index(name = "idx_enrollment_session_class_sec", columnList = "tenant_id, session_id, student_class, section"),
        @Index(name = "idx_enrollment_student", columnList = "tenant_id, student_id"),
        @Index(name = "idx_enrollment_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_enrollment_tenant_session_student", columnNames = {"tenant_id", "session_id", "student_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "session_code", nullable = false, length = 50)
    private String sessionCode;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "admission_no", length = 50)
    private String admissionNo;

    @Column(name = "student_class", nullable = false, length = 20)
    private String studentClass; // e.g. "10", "9", "KG"

    @Column(nullable = false, length = 10)
    private String section; // e.g. "A", "B"

    @Column(name = "roll_no", length = 50)
    private String rollNo;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "withdrawal_date")
    private LocalDate withdrawalDate;

    @Column(name = "withdrawal_reason", length = 255)
    private String withdrawalReason;

    @Column(length = 500)
    private String remarks;

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
            this.status = EnrollmentStatus.ACTIVE;
        }
        if (this.enrollmentDate == null) {
            this.enrollmentDate = LocalDate.now();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
