package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

    List<WorkflowState> findByWorkflowDefinitionIdOrderByDisplayOrderAsc(Long workflowDefinitionId);

    Optional<WorkflowState> findByWorkflowDefinitionIdAndStateKey(Long workflowDefinitionId, String stateKey);

    Optional<WorkflowState> findByWorkflowDefinitionIdAndIsInitialTrue(Long workflowDefinitionId);

    void deleteByWorkflowDefinitionId(Long workflowDefinitionId);
}
