package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_entitlements", indexes = {
        @Index(name = "idx_fe_tenant", columnList = "tenant_key"),
        @Index(name = "idx_fe_tenant_env", columnList = "tenant_key, environment_key"),
        @Index(name = "idx_fe_feature", columnList = "feature_key")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tenant_env_feature", columnNames = {"tenant_key", "environment_key", "feature_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, length = 64)
    private String tenantKey;

    @Column(name = "environment_key", length = 32)
    private String environmentKey; // Nullable: null means applies across all environments for this tenant

    @Column(name = "feature_key", nullable = false, length = 64)
    private String featureKey; // e.g. ATTENDANCE, FEES, EXAMINATIONS, etc.

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
