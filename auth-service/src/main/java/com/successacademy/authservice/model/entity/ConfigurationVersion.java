package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_versions", indexes = {
        @Index(name = "idx_cfg_ver_tenant", columnList = "tenant_key"),
        @Index(name = "idx_cfg_ver_env", columnList = "environment_key"),
        @Index(name = "idx_cfg_ver_status", columnList = "status"),
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
    private String status = "DRAFT"; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_by", length = 64)
    private String publishedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
    }
}
