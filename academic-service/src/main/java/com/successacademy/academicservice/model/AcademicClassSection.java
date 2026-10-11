package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_class_sections",
    indexes = {
        @Index(name = "idx_acs_tenant_session", columnList = "tenant_id, session_id"),
        @Index(name = "idx_acs_class", columnList = "tenant_id, class_code")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_acs_tenant_session_class_sec", columnNames = {"tenant_id", "session_id", "class_code", "section_code"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicClassSection {

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

    @Column(name = "section_code", nullable = false, length = 20)
    private String sectionCode;

    @Column(nullable = false)
    @Builder.Default
    private Integer capacity = 40;

    @Column(name = "room_number", length = 50)
    private String roomNumber;

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
        if (sectionCode != null) sectionCode = sectionCode.trim().toUpperCase();
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (classCode != null) classCode = classCode.trim().toUpperCase();
        if (sectionCode != null) sectionCode = sectionCode.trim().toUpperCase();
    }
}
