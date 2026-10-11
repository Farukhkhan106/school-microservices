package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_configurations", indexes = {
        @Index(name = "idx_tc_version", columnList = "version_id"),
        @Index(name = "idx_tc_tenant_cat", columnList = "tenant_key, category"),
        @Index(name = "idx_tc_tenant_env_cat", columnList = "tenant_key, environment_key, category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "tenant_key", nullable = false, length = 64)
    private String tenantKey;

    @Column(name = "environment_key", length = 32)
    private String environmentKey;

    @Column(name = "category", nullable = false, length = 64)
    private String category; // BRANDING, ORGANIZATION, FEATURES, NAVIGATION, DASHBOARD, BUSINESS

    @Lob
    @Column(name = "config_data", nullable = false, columnDefinition = "LONGTEXT")
    private String configData;

    @Column(name = "schema_version", nullable = false, length = 16)
    @Builder.Default
    private String schemaVersion = "v1";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (schemaVersion == null) schemaVersion = "v1";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
