package com.successacademy.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_audit_logs", indexes = {
        @Index(name = "idx_ual_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ual_target", columnList = "target_user_id"),
        @Index(name = "idx_ual_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "target_username", nullable = false, length = 100)
    private String targetUsername;

    @Column(nullable = false, length = 50)
    private String action; // USER_CREATED, USER_UPDATED, ROLE_ASSIGNED, ROLE_REVOKED, USER_ACTIVATED, USER_DEACTIVATED, USER_SUSPENDED, PASSWORD_RESET_INITIATED, PASSWORD_RESET_COMPLETED, INVITATION_SENT, INVITATION_ACCEPTED, PROTECTED_OP_DENIED

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
