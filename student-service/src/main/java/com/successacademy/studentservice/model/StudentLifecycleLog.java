package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_lifecycle_logs",
    indexes = {
        @Index(name = "idx_sll_tenant_student", columnList = "tenant_id, student_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLifecycleLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "from_status", length = 50)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 50)
    private String toStatus;

    @Column(nullable = false, length = 50)
    private String action; // ADMITTED, ACTIVATED, PROMOTED, RETAINED, TRANSFERRED, WITHDRAWN, GRADUATED, DEACTIVATED

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(length = 255)
    private String reason;

    @Column(length = 1000)
    private String notes;

    @Column(name = "destination_institution", length = 255)
    private String destinationInstitution;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String getPerformedBy() {
        return this.actorUsername;
    }

    public void setPerformedBy(String user) {
        this.actorUsername = user;
    }

    public LocalDateTime getPerformedAt() {
        return this.createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null || this.tenantId.isBlank()) this.tenantId = "default";
        if (this.effectiveDate == null) this.effectiveDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }
}
