package com.successacademy.authservice.model.entity.workflow;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workflow_instances", indexes = {
        @Index(name = "idx_wfi_tenant_state", columnList = "tenant_id, current_state"),
        @Index(name = "idx_wfi_created_by", columnList = "tenant_id, created_by_user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wfi_tenant_res", columnNames = {"tenant_id", "resource_type", "resource_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinition workflowDefinition;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion;

    @Column(name = "resource_type", nullable = false, length = 100)
    private String resourceType; // e.g. "LEAVE", "ASSESSMENT"

    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;

    @Column(name = "current_state", nullable = false, length = 50)
    private String currentState;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_by_username", length = 100)
    private String createdByUsername;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkflowAction> actions = new ArrayList<>();

    @OneToMany(mappedBy = "workflowInstance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApprovalTask> tasks = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
