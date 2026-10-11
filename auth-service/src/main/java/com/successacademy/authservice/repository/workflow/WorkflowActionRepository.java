package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.WorkflowAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowActionRepository extends JpaRepository<WorkflowAction, Long> {

    List<WorkflowAction> findByWorkflowInstanceIdOrderByCreatedAtAsc(Long workflowInstanceId);

    Optional<WorkflowAction> findByWorkflowInstanceIdAndIdempotencyKey(Long workflowInstanceId, String idempotencyKey);
}
