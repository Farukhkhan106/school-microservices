package com.successacademy.authservice.repository.workflow;

import com.successacademy.authservice.model.entity.workflow.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, Long> {

    List<WorkflowDefinition> findByTenantId(String tenantId);

    List<WorkflowDefinition> findByTenantIdIsNull();

    @Query("SELECT wd FROM WorkflowDefinition wd WHERE wd.tenantId = :tenantId OR wd.tenantId IS NULL ORDER BY wd.workflowKey, wd.version DESC")
    List<WorkflowDefinition> findAvailableForTenant(@Param("tenantId") String tenantId);

    @Query("SELECT wd FROM WorkflowDefinition wd WHERE (wd.tenantId = :tenantId OR wd.tenantId IS NULL) AND wd.workflowKey = :workflowKey AND wd.status = 'PUBLISHED' ORDER BY CASE WHEN wd.tenantId = :tenantId THEN 0 ELSE 1 END, wd.version DESC")
    List<WorkflowDefinition> findPublishedDefinitionsForTenant(@Param("tenantId") String tenantId, @Param("workflowKey") String workflowKey);

    Optional<WorkflowDefinition> findByWorkflowKeyAndVersionAndTenantId(String workflowKey, Integer version, String tenantId);

    Optional<WorkflowDefinition> findByWorkflowKeyAndVersionAndTenantIdIsNull(String workflowKey, Integer version);

    @Query("SELECT COALESCE(MAX(wd.version), 0) FROM WorkflowDefinition wd WHERE wd.workflowKey = :workflowKey AND ((:tenantId IS NULL AND wd.tenantId IS NULL) OR wd.tenantId = :tenantId)")
    Integer findMaxVersion(@Param("workflowKey") String workflowKey, @Param("tenantId") String tenantId);
}
