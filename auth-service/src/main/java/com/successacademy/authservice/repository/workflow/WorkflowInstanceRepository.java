package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {

    Optional<WorkflowInstance> findByIdAndTenantId(Long id, String tenantId);

    Optional<WorkflowInstance> findByTenantIdAndResourceTypeAndResourceId(String tenantId, String resourceType, String resourceId);

    List<WorkflowInstance> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<WorkflowInstance> findByTenantIdAndResourceTypeOrderByCreatedAtDesc(String tenantId, String resourceType);

    List<WorkflowInstance> findByTenantIdAndCreatedByUserIdOrderByCreatedAtDesc(String tenantId, Long createdByUserId);

    List<WorkflowInstance> findByTenantIdAndCurrentStateOrderByCreatedAtDesc(String tenantId, String currentState);
}
