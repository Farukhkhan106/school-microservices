package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_subjects",
    indexes = {
        @Index(name = "idx_subjects_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_subjects_tenant_order", columnList = "tenant_id, display_order")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_subject_code", columnNames = {"tenant_id", "subject_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "subject_code", nullable = false, length = 50)
    private String subjectCode; // e.g. "MATHEMATICS", "ENGLISH", "PHYSICS"

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName; // e.g. "Mathematics", "English"

    @Column(name = "subject_type", nullable = false, length = 30)
    @Builder.Default
    private String subjectType = "CORE"; // CORE, ELECTIVE, VOCATIONAL, CO_CURRICULAR

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, ARCHIVED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (subjectCode != null) subjectCode = subjectCode.trim().toUpperCase();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (subjectCode != null) subjectCode = subjectCode.trim().toUpperCase();
    }
}
