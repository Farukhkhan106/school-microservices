package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_sessions",
    indexes = {
        @Index(name = "idx_sessions_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_sessions_tenant_code", columnList = "tenant_id, session_code")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "session_code", nullable = false, length = 50)
    private String sessionCode; // e.g. "2026-2027"

    @Column(nullable = false, length = 100)
    private String name; // e.g. "Academic Session 2026-27"

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SessionStatus status = SessionStatus.UPCOMING;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(length = 500)
    private String description;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

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
            this.status = this.isActive ? SessionStatus.ACTIVE : SessionStatus.UPCOMING;
        }
        this.isActive = (this.status == SessionStatus.ACTIVE);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.status != null) {
            this.isActive = (this.status == SessionStatus.ACTIVE);
        }
        this.updatedAt = LocalDateTime.now();
    }
}
