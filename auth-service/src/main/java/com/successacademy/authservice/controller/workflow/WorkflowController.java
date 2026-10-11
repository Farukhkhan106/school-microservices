package com.successacademy.authservice.controller.workflow;

import com.successacademy.authservice.dto.workflow.*;
import com.successacademy.authservice.security.SecurityContextUtil;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.workflow.WorkflowDefinitionService;
import com.successacademy.authservice.service.workflow.WorkflowEngineService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping({"/workflow", "/auth-service/workflow"})
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowEngineService engineService;
    private final SecurityContextUtil securityContextUtil;

    // =========================================================================
    // WORKFLOW DEFINITIONS
    // =========================================================================

    @GetMapping("/definitions")
    public ResponseEntity<List<WorkflowDefinitionDto>> getDefinitions(HttpServletRequest request) {
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(definitionService.getDefinitionsForTenant(tenantId));
    }

    @GetMapping("/definitions/{id}")
    public ResponseEntity<WorkflowDefinitionDto> getDefinitionById(@PathVariable Long id, HttpServletRequest request) {
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(definitionService.getDefinitionById(id, tenantId));
    }

    @PostMapping("/definitions")
    public ResponseEntity<WorkflowDefinitionDto> createDefinition(
            @RequestBody CreateWorkflowDefinitionRequest body,
            HttpServletRequest request
    ) {
        requireWorkflowManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.createDraftDefinition(body, tenantId, username));
    }

    @PutMapping("/definitions/{id}")
    public ResponseEntity<WorkflowDefinitionDto> updateDefinition(
            @PathVariable Long id,
            @RequestBody UpdateWorkflowDefinitionRequest body,
            HttpServletRequest request
    ) {
        requireWorkflowManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(definitionService.updateDraftDefinition(id, body, tenantId, username));
    }

    @PostMapping("/definitions/{id}/versions")
    public ResponseEntity<WorkflowDefinitionDto> createNewVersion(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        requireWorkflowManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(definitionService.createNextVersion(id, tenantId, username));
    }

    @PostMapping("/definitions/{id}/publish")
    public ResponseEntity<WorkflowDefinitionDto> publishDefinition(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        requireWorkflowManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(definitionService.publishDefinition(id, tenantId, username));
    }

    @PostMapping("/definitions/{id}/archive")
    public ResponseEntity<WorkflowDefinitionDto> archiveDefinition(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        requireWorkflowManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(definitionService.archiveDefinition(id, tenantId, username));
    }

    // =========================================================================
    // WORKFLOW INSTANCES & ACTIONS
    // =========================================================================

    @PostMapping("/instances")
    public ResponseEntity<WorkflowInstanceDto> startWorkflow(
            @RequestBody StartWorkflowRequest body,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = getRequiredUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(engineService.startWorkflow(tenantId, body, userId, username));
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<WorkflowInstanceDto> getInstance(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = securityContextUtil.getCurrentUserId(request);
        return ResponseEntity.ok(engineService.getInstance(id, tenantId, userId));
    }

    @GetMapping("/instances/resource/{resourceType}/{resourceId}")
    public ResponseEntity<WorkflowInstanceDto> getInstanceByResource(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = securityContextUtil.getCurrentUserId(request);
        return ResponseEntity.ok(engineService.getInstanceByResource(tenantId, resourceType, resourceId, userId));
    }

    @PostMapping("/instances/{id}/actions")
    public ResponseEntity<WorkflowInstanceDto> executeTransition(
            @PathVariable Long id,
            @RequestBody ExecuteTransitionRequest body,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = getRequiredUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(engineService.executeTransition(id, body, userId, username, tenantId));
    }

    @GetMapping("/instances/{id}/history")
    public ResponseEntity<List<WorkflowHistoryDto>> getHistory(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(engineService.getHistory(id, tenantId));
    }

    // =========================================================================
    // APPROVAL TASKS INBOX & ACTIONS
    // =========================================================================

    @GetMapping("/tasks")
    public ResponseEntity<List<ApprovalTaskDto>> getPendingTasks(HttpServletRequest request) {
        String tenantId = getEffectiveTenant(request);
        Long userId = securityContextUtil.getCurrentUserId(request);
        String role = securityContextUtil.getCurrentRole(request);
        return ResponseEntity.ok(engineService.getPendingTasks(tenantId, userId, role));
    }

    @PostMapping("/tasks/{id}/approve")
    public ResponseEntity<WorkflowInstanceDto> approveTask(
            @PathVariable Long id,
            @RequestBody(required = false) TaskActionRequest body,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = getRequiredUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);
        TaskActionRequest req = (body != null) ? body : new TaskActionRequest();
        return ResponseEntity.ok(engineService.approveTask(id, req, userId, username, tenantId));
    }

    @PostMapping("/tasks/{id}/reject")
    public ResponseEntity<WorkflowInstanceDto> rejectTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = getRequiredUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(engineService.rejectTask(id, body, userId, username, tenantId));
    }

    @PostMapping("/tasks/{id}/return")
    public ResponseEntity<WorkflowInstanceDto> returnTask(
            @PathVariable Long id,
            @RequestBody TaskActionRequest body,
            HttpServletRequest request
    ) {
        String tenantId = getEffectiveTenant(request);
        Long userId = getRequiredUserId(request);
        String username = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(engineService.returnTask(id, body, userId, username, tenantId));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String getEffectiveTenant(HttpServletRequest request) {
        String tenant = securityContextUtil.getTenantId(request);
        return (tenant != null && !tenant.isBlank()) ? tenant : TenantContext.getTenantId();
    }

    private Long getRequiredUserId(HttpServletRequest request) {
        Long userId = securityContextUtil.getCurrentUserId(request);
        if (userId == null) {
            // Check if mock/testing header or fallback to 1L
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    return Long.parseLong(userIdHeader.trim());
                } catch (NumberFormatException ignored) {}
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User authentication required.");
        }
        return userId;
    }

    private void requireWorkflowManager(HttpServletRequest request) {
        String role = securityContextUtil.getCurrentRole(request);
        if ("ADMIN".equalsIgnoreCase(role)
                || "TENANT_ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role)
                || "PLATFORM_ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Workflow administrative privileges required.");
    }
}
