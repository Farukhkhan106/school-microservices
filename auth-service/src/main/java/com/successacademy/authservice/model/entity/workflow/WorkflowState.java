package com.successacademy.authservice.model.entity.workflow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workflow_states", indexes = {
        @Index(name = "idx_wfs_def", columnList = "workflow_definition_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wfs_def_state", columnNames = {"workflow_definition_id", "state_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    @JsonIgnore
    private WorkflowDefinition workflowDefinition;

    @Column(name = "state_key", nullable = false, length = 50)
    private String stateKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "state_type", nullable = false, length = 30)
    @Builder.Default
    private String stateType = "INTERMEDIATE"; // INITIAL, INTERMEDIATE, TERMINAL

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_initial", nullable = false)
    @Builder.Default
    private boolean isInitial = false;

    @Column(name = "is_terminal", nullable = false)
    @Builder.Default
    private boolean isTerminal = false;
}
