package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "permissions", indexes = {
        @Index(name = "idx_perm_key", columnList = "permission_key"),
        @Index(name = "idx_perm_cat", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String permissionKey; // e.g. academic.class.create, student.view

    @Column(name = "resource_name", nullable = false, length = 50)
    private String resourceName;

    @Column(name = "action_name", nullable = false, length = 50)
    private String actionName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // ACADEMIC, STUDENT, FACULTY, STAFF, SECURITY, etc.

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = true;

    @Column(name = "is_protected", nullable = false)
    @Builder.Default
    private boolean isProtected = false; // True for platform/ownership operations

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
