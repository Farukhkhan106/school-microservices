package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_classes",
    indexes = {
        @Index(name = "idx_classes_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_classes_tenant_order", columnList = "tenant_id, display_order")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_class_code", columnNames = {"tenant_id", "class_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "class_code", nullable = false, length = 30)
    private String classCode; // e.g. "NURSERY", "1", "10", "GRADE_1"

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName; // e.g. "Nursery", "Class 1", "Class 10"

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String category = "PRIMARY"; // PRE_PRIMARY, PRIMARY, MIDDLE, SECONDARY, SENIOR_SECONDARY

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
        if (classCode != null) classCode = classCode.trim().toUpperCase();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (classCode != null) classCode = classCode.trim().toUpperCase();
    }
}
