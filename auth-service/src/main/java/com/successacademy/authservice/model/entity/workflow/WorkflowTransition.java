package com.successacademy.authservice.model.entity.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_transitions", indexes = {
        @Index(name = "idx_wft_def", columnList = "workflow_definition_id"),
        @Index(name = "idx_wft_action", columnList = "action_key")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wft_def_trans", columnNames = {"workflow_definition_id", "from_state", "action_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    @JsonIgnore
    private WorkflowDefinition workflowDefinition;

    @Column(name = "from_state", nullable = false, length = 50)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 50)
    private String toState;

    @Column(name = "action_key", nullable = false, length = 50)
    private String actionKey; // e.g. "SUBMIT", "APPROVE", "REJECT", "RETURN", "CANCEL"

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey; // e.g. "leave.approve", "leave.submit"

    @Column(name = "required_scope", length = 50)
    @Builder.Default
    private String requiredScope = "TENANT"; // OWN, ASSIGNED, TENANT, ALL

    @Column(name = "approval_required", nullable = false)
    @Builder.Default
    private boolean approvalRequired = false;

    @Column(name = "approval_level", nullable = false)
    @Builder.Default
    private Integer approvalLevel = 1;

    @Column(name = "maker_checker_required", nullable = false)
    @Builder.Default
    private boolean makerCheckerRequired = false;

    @Column(name = "requires_comment", nullable = false)
    @Builder.Default
    private boolean requiresComment = false;

    @Column(name = "requires_rejection_reason", nullable = false)
    @Builder.Default
    private boolean requiresRejectionReason = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
