package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_guardians",
    indexes = {
        @Index(name = "idx_sg_tenant_guardian", columnList = "tenant_id, guardian_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_sg_tenant_student_guardian", columnNames = {"tenant_id", "student_id", "guardian_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGuardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "guardian_id", nullable = false)
    private Long guardianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private GuardianRelationship relationshipType;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_emergency_contact")
    @Builder.Default
    private Boolean isEmergencyContact = true;

    @Column(name = "can_pickup")
    @Builder.Default
    private Boolean canPickup = true;

    @Column(name = "can_receive_notifications")
    @Builder.Default
    private Boolean canReceiveNotifications = true;

    @Column(name = "can_access_portal")
    @Builder.Default
    private Boolean canAccessPortal = false;

    @Column(length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null || this.tenantId.isBlank()) this.tenantId = "default";
        if (this.status == null) this.status = "ACTIVE";
        if (this.isPrimary == null) this.isPrimary = false;
        if (this.isEmergencyContact == null) this.isEmergencyContact = true;
        if (this.canPickup == null) this.canPickup = true;
        if (this.canReceiveNotifications == null) this.canReceiveNotifications = true;
        if (this.canAccessPortal == null) this.canAccessPortal = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
