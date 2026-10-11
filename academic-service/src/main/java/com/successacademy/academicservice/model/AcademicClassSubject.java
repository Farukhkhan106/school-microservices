package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_class_subjects",
    indexes = {
        @Index(name = "idx_acsubj_tenant_session", columnList = "tenant_id, session_id"),
        @Index(name = "idx_acsubj_class", columnList = "tenant_id, class_code")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_acsubj_tenant_session_class_subj", columnNames = {"tenant_id", "session_id", "class_code", "subject_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicClassSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "class_code", nullable = false, length = 30)
    private String classCode;

    @Column(name = "subject_code", nullable = false, length = 50)
    private String subjectCode;

    @Column(name = "is_mandatory", nullable = false)
    @Builder.Default
    private boolean isMandatory = true;

    @Column(name = "weekly_periods", nullable = false)
    @Builder.Default
    private Integer weeklyPeriods = 5;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (classCode != null) classCode = classCode.trim().toUpperCase();
        if (subjectCode != null) subjectCode = subjectCode.trim().toUpperCase();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (classCode != null) classCode = classCode.trim().toUpperCase();
        if (subjectCode != null) subjectCode = subjectCode.trim().toUpperCase();
    }
}
