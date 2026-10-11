package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles", indexes = {
        @Index(name = "idx_roles_tenant", columnList = "tenant_id"),
        @Index(name = "idx_roles_code", columnList = "role_code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_tenant_code", columnNames = {"tenant_id", "role_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", length = 64)
    private String tenantId; // NULL for global system roles; non-null for tenant-specific custom roles

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode; // e.g. TENANT_ADMIN, ADMIN_DELEGATE, TEACHER, CUSTOM_FEE_MGR

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    @Column(name = "is_protected", nullable = false)
    @Builder.Default
    private boolean isProtected = false; // Protected system roles cannot be altered or assigned without platform privileges

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

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
