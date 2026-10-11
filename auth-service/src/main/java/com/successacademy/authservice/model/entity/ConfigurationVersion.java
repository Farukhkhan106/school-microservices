package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_versions", indexes = {
        @Index(name = "idx_cfg_ver_tenant", columnList = "tenant_key"),
        @Index(name = "idx_cfg_ver_env", columnList = "environment_key"),
        @Index(name = "idx_cfg_ver_status", columnList = "status"),
        @Index(name = "idx_cfg_ver_version", columnList = "version_number"),
        @Index(name = "idx_cfg_ver_tenant_status", columnList = "tenant_key, environment_key, status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_env_version", columnNames = {"tenant_key", "environment_key", "version_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64)
    private String tenantKey;

    @Column(name = "environment_key", length = 32)
    private String environmentKey; // Nullable: null applies across all environments for this tenant

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "DRAFT"; // DRAFT, VALIDATING, PENDING_APPROVAL, APPROVED, PUBLISHED, ARCHIVED, REJECTED

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @Column(name = "parent_version_id")
    private Long parentVersionId;

    @Column(name = "rollback_from_version")
    private Integer rollbackFromVersion;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Version
    @Column(name = "opt_lock_version", nullable = false)
    @Builder.Default
    private Long optLockVersion = 0L;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", length = 64)
    @Builder.Default
    private String updatedBy = "system";

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_by", length = 64)
    private String publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "DRAFT";
        if (optLockVersion == null) optLockVersion = 0L;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
