package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.WorkflowAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowAuditRepository extends JpaRepository<WorkflowAudit, Long> {

    List<WorkflowAudit> findByWorkflowInstanceIdOrderByCreatedAtAsc(Long workflowInstanceId);

    List<WorkflowAudit> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
