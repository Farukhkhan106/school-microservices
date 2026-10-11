package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "guardians",
    indexes = {
        @Index(name = "idx_guardian_tenant_phone", columnList = "tenant_id, phone"),
        @Index(name = "idx_guardian_tenant_email", columnList = "tenant_id, email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "user_id")
    private Long userId; // FK → authdb.users.id if portal login enabled

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(name = "alternate_phone", length = 30)
    private String alternatePhone;

    @Column(length = 100)
    private String occupation;

    @Column(name = "relationship_metadata", length = 255)
    private String relationshipMetadata;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String pincode;

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
        if (this.displayName == null || this.displayName.isBlank()) {
            this.displayName = (this.firstName + " " + (this.lastName != null ? this.lastName : "")).trim();
        }
        if (this.status == null) this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
