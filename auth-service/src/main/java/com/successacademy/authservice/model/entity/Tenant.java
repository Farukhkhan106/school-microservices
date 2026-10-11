package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_key", columnList = "tenant_key", unique = true),
        @Index(name = "idx_tenant_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_key", nullable = false, unique = true, length = 64)
    private String tenantKey;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, INACTIVE, PENDING

    @Column(name = "domain", length = 128)
    private String domain;

    @Column(name = "contact_email", length = 128)
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
