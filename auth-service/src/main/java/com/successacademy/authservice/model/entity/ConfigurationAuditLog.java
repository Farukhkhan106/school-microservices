package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_audit_logs", indexes = {
        @Index(name = "idx_cal_tenant", columnList = "tenant_key"),
        @Index(name = "idx_cal_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64)
    private String tenantKey;

    @Column(name = "environment_key", length = 32)
    private String environmentKey;

    @Column(name = "actor_username", nullable = false, length = 64)
    private String actorUsername;

    @Column(name = "actor_role", length = 64)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 64)
    private String action; // CREATE_DRAFT, UPDATE_DRAFT, VALIDATE, PUBLISH, ROLLBACK, ARCHIVE, FEATURE_ENABLE, FEATURE_DISABLE, IMPORT, EXPORT, APPROVAL_REQUESTED, APPROVED, REJECTED

    @Column(name = "target_version_number")
    private Integer targetVersionNumber;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "diff_summary", columnDefinition = "TEXT")
    private String diffSummary;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
