package com.successacademy.authservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users", indexes = {
        @Index(name = "idx_user_tenant", columnList = "tenant_id"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_status", columnList = "status"),
        @Index(name = "idx_user_type", columnList = "user_type")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @JsonIgnore
    @Column(nullable = false)
    private String password;   // BCrypt encoded

    @Column(length = 50)
    private String role;       // Legacy role code (TENANT_ADMIN / STUDENT / TEACHER / etc.)

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "user_type", length = 50)
    @Builder.Default
    private String userType = "STAFF"; // PLATFORM_ADMIN, TENANT_ADMIN, ADMIN_DELEGATE, PRINCIPAL, TEACHER, STAFF, SUPPORT, STUDENT, GUARDIAN

    // Links to other service entities (nullable)
    @Column(name = "student_id")
    private Long studentId;    // FK → student-service students.id

    @Column(name = "teacher_id")
    private Long teacherId;    // FK → faculty-service faculty.id

    @Column(name = "staff_id")
    private Long staffId;      // FK → staff-service staff.id

    @Column(name = "guardian_id")
    private Long guardianId;   // FK → student-service guardians.id

    // Multi-tenant key
    @Column(name = "tenant_id", length = 64, nullable = false)
    @Builder.Default
    private String tenantId = "default";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // INVITED, ACTIVE, INACTIVE, SUSPENDED, LOCKED

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    @Builder.Default
    private String createdBy = "SYSTEM";

    @Column(name = "updated_by", length = 100)
    @Builder.Default
    private String updatedBy = "SYSTEM";

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (displayName == null || displayName.isBlank()) {
            displayName = (firstName != null && !firstName.isBlank())
                    ? (firstName + (lastName != null ? " " + lastName : ""))
                    : username;
        }
        if (status == null || status.isBlank()) status = "ACTIVE";
        if (tenantId == null || tenantId.isBlank()) tenantId = "default";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (displayName == null || displayName.isBlank()) {
            displayName = (firstName != null && !firstName.isBlank())
                    ? (firstName + (lastName != null ? " " + lastName : ""))
                    : username;
        }
    }
}
