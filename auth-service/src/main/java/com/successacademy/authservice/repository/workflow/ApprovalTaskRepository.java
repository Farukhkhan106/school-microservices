package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.ApprovalTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalTaskRepository extends JpaRepository<ApprovalTask, Long> {

    Optional<ApprovalTask> findByIdAndTenantId(Long id, String tenantId);

    List<ApprovalTask> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);

    List<ApprovalTask> findByWorkflowInstanceIdOrderByCreatedAtAsc(Long workflowInstanceId);

    List<ApprovalTask> findByWorkflowInstanceIdAndStatus(Long workflowInstanceId, String status);

    @Query("SELECT t FROM ApprovalTask t WHERE t.tenantId = :tenantId AND t.status = 'PENDING' AND (t.assignedTo = :userId OR (t.assignedTo IS NULL AND (:role IS NULL OR t.assignedRole = :role OR t.assignedRole IS NULL))) ORDER BY t.createdAt DESC")
    List<ApprovalTask> findPendingTasksForUser(@Param("tenantId") String tenantId, @Param("userId") Long userId, @Param("role") String role);
}
