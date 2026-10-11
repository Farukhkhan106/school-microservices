package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findByWorkflowDefinitionId(Long workflowDefinitionId);

    List<WorkflowTransition> findByWorkflowDefinitionIdAndFromStateAndIsActiveTrue(Long workflowDefinitionId, String fromState);

    Optional<WorkflowTransition> findByWorkflowDefinitionIdAndFromStateAndActionKey(Long workflowDefinitionId, String fromState, String actionKey);

    void deleteByWorkflowDefinitionId(Long workflowDefinitionId);
}
