package com.successacademy.authservice.model.entity.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_tasks", indexes = {
        @Index(name = "idx_apt_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_apt_inst", columnList = "workflow_instance_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_instance_id", nullable = false)
    @JsonIgnore
    private WorkflowInstance workflowInstance;

    @Column(name = "transition_id")
    private Long transitionId;

    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 1;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, RETURNED, CANCELLED

    @Column(name = "assigned_role", length = 100)
    private String assignedRole;

    @Column(name = "assigned_to")
    private Long assignedTo; // null if open to role pool, or specific user ID

    @Column(name = "assigned_scope", length = 50)
    @Builder.Default
    private String assignedScope = "TENANT";

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_by_username", length = 100)
    private String completedByUsername;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (version == null) {
            version = 0L;
        }
    }
}
