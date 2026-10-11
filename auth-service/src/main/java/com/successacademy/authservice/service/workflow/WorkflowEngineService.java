package com.successacademy.authservice.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.workflow.*;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.model.entity.workflow.*;
import com.successacademy.authservice.repository.workflow.*;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.rbac.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngineService {

    private final WorkflowDefinitionService definitionService;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowActionRepository actionRepository;
    private final ApprovalTaskRepository taskRepository;
    private final WorkflowAuditRepository auditRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowStateRepository stateRepository;
    private final AuthorizationService authorizationService;
    private final FeatureEntitlementService featureEntitlementService;
    private final ObjectMapper objectMapper;

    /**
     * Start a new workflow instance for a business resource.
     */
    @Transactional
    public WorkflowInstanceDto startWorkflow(String tenantId, StartWorkflowRequest req, Long actorUserId, String actorUsername) {
        String cleanTenant = cleanTenant(tenantId);
        String wfKey = req.getWorkflowKey().trim().toUpperCase();
        String resType = req.getResourceType().trim().toUpperCase();
        String resId = req.getResourceId().trim();

        // 1. Resolve effective workflow definition
        WorkflowDefinition def = definitionService.findEffectiveDefinition(cleanTenant, wfKey);

        // 2. Feature Entitlement check
        if (def.getFeatureKey() != null && !def.getFeatureKey().isBlank()) {
            boolean enabled = featureEntitlementService.isFeatureEnabled(
                    cleanTenant,
                    featureEntitlementService.getRuntimeEnvironment(),
                    def.getFeatureKey()
            );
            if (!enabled) {
                log.warn("Workflow start blocked: Feature '{}' is disabled for tenant '{}'", def.getFeatureKey(), cleanTenant);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Feature '" + def.getFeatureKey() + "' is disabled for this school tenant.");
            }
        }

        // 3. Find initial state
        WorkflowState initialState = stateRepository.findByWorkflowDefinitionIdAndIsInitialTrue(def.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Workflow definition '" + wfKey + "' has no configured initial state."));

        // 4. Idempotency & Duplicate instance check
        Optional<WorkflowInstance> existingOpt = instanceRepository.findByTenantIdAndResourceTypeAndResourceId(cleanTenant, resType, resId);
        if (existingOpt.isPresent()) {
            WorkflowInstance existing = existingOpt.get();
            log.info("Workflow instance already exists for resource {}:{} in tenant {}", resType, resId, cleanTenant);
            return toInstanceDto(existing, actorUserId);
        }

        // 5. Create WorkflowInstance
        WorkflowInstance instance = WorkflowInstance.builder()
                .tenantId(cleanTenant)
                .workflowDefinition(def)
                .workflowVersion(def.getVersion())
                .resourceType(resType)
                .resourceId(resId)
                .currentState(initialState.getStateKey())
                .createdByUserId(actorUserId)
                .createdByUsername(actorUsername != null ? actorUsername : "USER")
                .version(0L)
                .build();

        WorkflowInstance saved = instanceRepository.save(instance);

        // 6. Record Initial Action & Audit
        WorkflowAction initialAction = WorkflowAction.builder()
                .workflowInstance(saved)
                .actionKey("START")
                .actorUserId(actorUserId)
                .actorUsername(actorUsername)
                .fromState("NONE")
                .toState(initialState.getStateKey())
                .comment(req.getInitialComment() != null ? req.getInitialComment() : "Workflow started")
                .idempotencyKey(req.getIdempotencyKey())
                .build();
        actionRepository.save(initialAction);

        createAudit(saved, "START", "NONE", initialState.getStateKey(), actorUserId, actorUsername, Map.of(
                "action", "START",
                "workflowKey", wfKey,
                "version", def.getVersion()
        ));

        log.info("Started workflow '{}' v{} for {}:{} (instance id {})", wfKey, def.getVersion(), resType, resId, saved.getId());
        return toInstanceDto(saved, actorUserId);
    }

    /**
     * Get a workflow instance by ID with available transitions.
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceDto getInstance(Long instanceId, String tenantId, Long actorUserId) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowInstance instance = instanceRepository.findByIdAndTenantId(instanceId, cleanTenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found: " + instanceId));

        return toInstanceDto(instance, actorUserId);
    }

    /**
     * Get workflow instance by business resource.
     */
    @Transactional(readOnly = true)
    public WorkflowInstanceDto getInstanceByResource(String tenantId, String resourceType, String resourceId, Long actorUserId) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowInstance instance = instanceRepository.findByTenantIdAndResourceTypeAndResourceId(
                cleanTenant, resourceType.trim().toUpperCase(), resourceId.trim()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Workflow instance not found for resource " + resourceType + ":" + resourceId));

        return toInstanceDto(instance, actorUserId);
    }

    /**
     * Centralized transition execution engine.
     * Enforces: Tenant -> Feature -> Permission -> Scope -> State -> Maker/Checker -> Idempotency -> Optimistic Lock -> Audit.
     */
    @Transactional
    public WorkflowInstanceDto executeTransition(Long instanceId, ExecuteTransitionRequest req, Long actorUserId, String actorUsername, String tenantId) {
        String cleanTenant = cleanTenant(tenantId);
        String actionKey = req.getActionKey().trim().toUpperCase();

        // 1. Tenant Isolation
        WorkflowInstance instance = instanceRepository.findByIdAndTenantId(instanceId, cleanTenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found: " + instanceId));

        // 2. Concurrency Control (Optimistic Locking Check)
        if (req.getExpectedVersion() != null && !req.getExpectedVersion().equals(instance.getVersion())) {
            log.warn("Concurrency conflict on instance {}: expected version {} but found {}", instanceId, req.getExpectedVersion(), instance.getVersion());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrent modification conflict: Workflow state has been changed by another user. Please refresh and retry.");
        }

        // 3. Idempotency Protection
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            Optional<WorkflowAction> existingAction = actionRepository.findByWorkflowInstanceIdAndIdempotencyKey(instanceId, req.getIdempotencyKey().trim());
            if (existingAction.isPresent()) {
                log.info("Idempotent transition request detected for instance {} with key '{}'", instanceId, req.getIdempotencyKey());
                return toInstanceDto(instance, actorUserId);
            }
        }

        // 4. Feature Entitlement Check
        WorkflowDefinition def = instance.getWorkflowDefinition();
        if (def.getFeatureKey() != null && !def.getFeatureKey().isBlank()) {
            boolean enabled = featureEntitlementService.isFeatureEnabled(
                    cleanTenant,
                    featureEntitlementService.getRuntimeEnvironment(),
                    def.getFeatureKey()
            );
            if (!enabled) {
                log.warn("Workflow action blocked: Feature '{}' is disabled for tenant '{}'", def.getFeatureKey(), cleanTenant);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Feature '" + def.getFeatureKey() + "' is disabled for this school tenant.");
            }
        }

        // 5. State and Transition Validation
        WorkflowTransition transition = transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(
                def.getId(), instance.getCurrentState(), actionKey
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid transition: Action '" + actionKey + "' is not permitted from current state '" + instance.getCurrentState() + "'"));

        if (!transition.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transition '" + transition.getName() + "' is currently disabled.");
        }

        // 6. Maker/Checker Rule Enforcement (Server-Side)
        if (transition.isMakerCheckerRequired()) {
            if (instance.getCreatedByUserId() != null && instance.getCreatedByUserId().equals(actorUserId)) {
                log.warn("Maker-checker violation: User {} attempted self-approval/action on instance {}", actorUserId, instanceId);
                createAudit(instance, "SECURITY_VIOLATION_MAKER_CHECKER", instance.getCurrentState(), instance.getCurrentState(),
                        actorUserId, actorUsername, Map.of(
                                "attemptedAction", actionKey,
                                "reason", "Maker cannot approve their own workflow instance"
                        ));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Maker-checker violation: Workflow creator cannot approve or execute checker action on their own request.");
            }
        }

        // 7. Phase 3 Permission & Scope Check
        ScopeType requestedScope = ScopeType.TENANT;
        if (transition.getRequiredScope() != null) {
            try {
                requestedScope = ScopeType.valueOf(transition.getRequiredScope().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                requestedScope = ScopeType.TENANT;
            }
        }

        Map<String, Object> resourceCtx = (req.getResourceContext() != null) ? new HashMap<>(req.getResourceContext()) : new HashMap<>();
        resourceCtx.put("ownerUserId", instance.getCreatedByUserId());
        if (def.getFeatureKey() != null) {
            resourceCtx.put("featureKey", def.getFeatureKey());
        }

        boolean authorized = authorizationService.isAllowed(actorUserId, cleanTenant, transition.getPermissionKey(), requestedScope, resourceCtx);
        if (!authorized) {
            log.warn("Workflow authorization denied: User {} lacks permission '{}' or scope '{}' for tenant '{}'",
                    actorUserId, transition.getPermissionKey(), requestedScope, cleanTenant);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: You do not possess the required permission ('" + transition.getPermissionKey() + "') or scope to execute this action.");
        }

        // 8. Rejection Reason and Comment Validation
        if (transition.isRequiresRejectionReason() && ("REJECT".equals(actionKey) || actionKey.contains("REJECT"))) {
            if (req.getReason() == null || req.getReason().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required for this transition.");
            }
        }

        if (transition.isRequiresComment()) {
            if (req.getComment() == null || req.getComment().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A comment is required for this transition.");
            }
        }

        // 9. Execute Transition & Handle Approval Tasks
        String fromState = instance.getCurrentState();
        String toState = transition.getToState();

        // Complete any pending tasks associated with this instance
        List<ApprovalTask> pendingTasks = taskRepository.findByWorkflowInstanceIdAndStatus(instanceId, "PENDING");
        for (ApprovalTask pt : pendingTasks) {
            if ("REJECT".equals(actionKey) || actionKey.contains("REJECT")) {
                pt.setStatus("REJECTED");
            } else if ("RETURN".equals(actionKey) || actionKey.contains("RETURN")) {
                pt.setStatus("RETURNED");
            } else if ("CANCEL".equals(actionKey) || actionKey.contains("CANCEL")) {
                pt.setStatus("CANCELLED");
            } else {
                pt.setStatus("APPROVED");
            }
            pt.setCompletedAt(LocalDateTime.now());
            pt.setCompletedBy(actorUserId);
            pt.setCompletedByUsername(actorUsername);
            pt.setComment(req.getComment() != null ? req.getComment() : req.getReason());
            taskRepository.save(pt);
        }

        // If transition creates a new pending approval task (e.g. SUBMIT -> PENDING_APPROVAL)
        if (transition.isApprovalRequired() && !"APPROVED".equalsIgnoreCase(toState) && !"REJECTED".equalsIgnoreCase(toState)) {
            ApprovalTask newTask = ApprovalTask.builder()
                    .tenantId(cleanTenant)
                    .workflowInstance(instance)
                    .transitionId(transition.getId())
                    .level(transition.getApprovalLevel() != null ? transition.getApprovalLevel() : 1)
                    .status("PENDING")
                    .assignedRole("ADMIN") // Default role pool
                    .assignedScope(transition.getRequiredScope() != null ? transition.getRequiredScope() : "TENANT")
                    .build();
            taskRepository.save(newTask);
            log.info("Created approval task level {} for instance {}", newTask.getLevel(), instanceId);
        } else if ("PENDING_APPROVAL".equalsIgnoreCase(toState)) {
            // Ensure task exists for pending approval
            ApprovalTask newTask = ApprovalTask.builder()
                    .tenantId(cleanTenant)
                    .workflowInstance(instance)
                    .transitionId(transition.getId())
                    .level(1)
                    .status("PENDING")
                    .assignedRole("ADMIN")
                    .assignedScope("TENANT")
                    .build();
            taskRepository.save(newTask);
            log.info("Created approval task for state PENDING_APPROVAL on instance {}", instanceId);
        }

        // 10. Update Instance State
        instance.setCurrentState(toState);
        instance.setUpdatedAt(LocalDateTime.now());
        WorkflowInstance updatedInstance = instanceRepository.save(instance);

        // 11. Record Action & Audit
        WorkflowAction action = WorkflowAction.builder()
                .workflowInstance(updatedInstance)
                .actionKey(actionKey)
                .actorUserId(actorUserId)
                .actorUsername(actorUsername)
                .fromState(fromState)
                .toState(toState)
                .comment(req.getComment())
                .reason(req.getReason())
                .idempotencyKey(req.getIdempotencyKey())
                .build();
        actionRepository.save(action);

        createAudit(updatedInstance, actionKey, fromState, toState, actorUserId, actorUsername, Map.of(
                "action", actionKey,
                "fromState", fromState,
                "toState", toState,
                "comment", req.getComment() != null ? req.getComment() : "",
                "reason", req.getReason() != null ? req.getReason() : "",
                "idempotencyKey", req.getIdempotencyKey() != null ? req.getIdempotencyKey() : ""
        ));

        log.info("Executed transition '{}': {} -> {} on instance {} by user {}",
                actionKey, fromState, toState, instanceId, actorUserId);

        return toInstanceDto(updatedInstance, actorUserId);
    }

    /**
     * Get pending tasks for approval inbox.
     * Evaluates maker-checker to omit tasks created by the actor.
     */
    @Transactional(readOnly = true)
    public List<ApprovalTaskDto> getPendingTasks(String tenantId, Long actorUserId, String actorRole) {
        String cleanTenant = cleanTenant(tenantId);
        List<ApprovalTask> tasks = taskRepository.findPendingTasksForUser(cleanTenant, actorUserId, actorRole);

        return tasks.stream()
                .filter(t -> {
                    // Filter out maker's own requests if maker-checker is enabled for the transition
                    WorkflowInstance inst = t.getWorkflowInstance();
                    if (inst != null && inst.getCreatedByUserId() != null && inst.getCreatedByUserId().equals(actorUserId)) {
                        // Check if any available transition requires maker-checker
                        List<WorkflowTransition> trans = transitionRepository.findByWorkflowDefinitionIdAndFromStateAndIsActiveTrue(
                                inst.getWorkflowDefinition().getId(), inst.getCurrentState()
                        );
                        boolean anyRequiresMc = trans.stream().anyMatch(WorkflowTransition::isMakerCheckerRequired);
                        if (anyRequiresMc) {
                            return false; // Maker cannot see/approve their own task
                        }
                    }
                    return true;
                })
                .map(this::toTaskDto)
                .collect(Collectors.toList());
    }

    /**
     * Approve a task from inbox.
     */
    @Transactional
    public WorkflowInstanceDto approveTask(Long taskId, TaskActionRequest req, Long actorUserId, String actorUsername, String tenantId) {
        ApprovalTask task = getTaskAndVerifyTenant(taskId, tenantId);
        ExecuteTransitionRequest transReq = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .comment(req.getComment())
                .idempotencyKey(req.getIdempotencyKey())
                .expectedVersion(req.getExpectedVersion())
                .resourceContext(req.getResourceContext())
                .build();
        return executeTransition(task.getWorkflowInstance().getId(), transReq, actorUserId, actorUsername, tenantId);
    }

    /**
     * Reject a task from inbox.
     */
    @Transactional
    public WorkflowInstanceDto rejectTask(Long taskId, TaskActionRequest req, Long actorUserId, String actorUsername, String tenantId) {
        ApprovalTask task = getTaskAndVerifyTenant(taskId, tenantId);
        ExecuteTransitionRequest transReq = ExecuteTransitionRequest.builder()
                .actionKey("REJECT")
                .comment(req.getComment())
                .reason(req.getReason())
                .idempotencyKey(req.getIdempotencyKey())
                .expectedVersion(req.getExpectedVersion())
                .resourceContext(req.getResourceContext())
                .build();
        return executeTransition(task.getWorkflowInstance().getId(), transReq, actorUserId, actorUsername, tenantId);
    }

    /**
     * Return a task for changes.
     */
    @Transactional
    public WorkflowInstanceDto returnTask(Long taskId, TaskActionRequest req, Long actorUserId, String actorUsername, String tenantId) {
        ApprovalTask task = getTaskAndVerifyTenant(taskId, tenantId);
        ExecuteTransitionRequest transReq = ExecuteTransitionRequest.builder()
                .actionKey("RETURN")
                .comment(req.getComment())
                .reason(req.getReason())
                .idempotencyKey(req.getIdempotencyKey())
                .expectedVersion(req.getExpectedVersion())
                .resourceContext(req.getResourceContext())
                .build();
        return executeTransition(task.getWorkflowInstance().getId(), transReq, actorUserId, actorUsername, tenantId);
    }

    /**
     * Get history for a workflow instance.
     */
    @Transactional(readOnly = true)
    public List<WorkflowHistoryDto> getHistory(Long instanceId, String tenantId) {
        String cleanTenant = cleanTenant(tenantId);
        WorkflowInstance instance = instanceRepository.findByIdAndTenantId(instanceId, cleanTenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow instance not found: " + instanceId));

        List<WorkflowAction> actions = actionRepository.findByWorkflowInstanceIdOrderByCreatedAtAsc(instance.getId());
        return actions.stream().map(a -> WorkflowHistoryDto.builder()
                .id(a.getId())
                .workflowInstanceId(instance.getId())
                .actionKey(a.getActionKey())
                .actorUserId(a.getActorUserId())
                .actorUsername(a.getActorUsername())
                .fromState(a.getFromState())
                .toState(a.getToState())
                .comment(a.getComment())
                .reason(a.getReason())
                .idempotencyKey(a.getIdempotencyKey())
                .createdAt(a.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    private ApprovalTask getTaskAndVerifyTenant(Long taskId, String tenantId) {
        String cleanTenant = cleanTenant(tenantId);
        return taskRepository.findByIdAndTenantId(taskId, cleanTenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval task not found: " + taskId));
    }

    private WorkflowInstanceDto toInstanceDto(WorkflowInstance inst, Long actorUserId) {
        List<WorkflowTransition> available = transitionRepository.findByWorkflowDefinitionIdAndFromStateAndIsActiveTrue(
                inst.getWorkflowDefinition().getId(), inst.getCurrentState()
        );

        // Filter out maker-checker blocked transitions for this actor in the UI
        List<WorkflowTransitionDto> transDtos = available.stream()
                .filter(t -> {
                    if (t.isMakerCheckerRequired() && actorUserId != null && actorUserId.equals(inst.getCreatedByUserId())) {
                        return false; // Maker cannot execute this transition
                    }
                    return true;
                })
                .map(t -> WorkflowTransitionDto.builder()
                        .id(t.getId())
                        .fromState(t.getFromState())
                        .toState(t.getToState())
                        .actionKey(t.getActionKey())
                        .name(t.getName())
                        .permissionKey(t.getPermissionKey())
                        .requiredScope(t.getRequiredScope())
                        .approvalRequired(t.isApprovalRequired())
                        .approvalLevel(t.getApprovalLevel())
                        .makerCheckerRequired(t.isMakerCheckerRequired())
                        .requiresComment(t.isRequiresComment())
                        .requiresRejectionReason(t.isRequiresRejectionReason())
                        .isActive(t.isActive())
                        .build())
                .collect(Collectors.toList());

        return WorkflowInstanceDto.builder()
                .id(inst.getId())
                .tenantId(inst.getTenantId())
                .workflowDefinitionId(inst.getWorkflowDefinition().getId())
                .workflowKey(inst.getWorkflowDefinition().getWorkflowKey())
                .workflowName(inst.getWorkflowDefinition().getName())
                .workflowVersion(inst.getWorkflowVersion())
                .resourceType(inst.getResourceType())
                .resourceId(inst.getResourceId())
                .currentState(inst.getCurrentState())
                .createdByUserId(inst.getCreatedByUserId())
                .createdByUsername(inst.getCreatedByUsername())
                .version(inst.getVersion())
                .createdAt(inst.getCreatedAt())
                .updatedAt(inst.getUpdatedAt())
                .availableTransitions(transDtos)
                .build();
    }

    private ApprovalTaskDto toTaskDto(ApprovalTask t) {
        WorkflowInstance inst = t.getWorkflowInstance();
        return ApprovalTaskDto.builder()
                .id(t.getId())
                .tenantId(t.getTenantId())
                .workflowInstanceId(inst != null ? inst.getId() : null)
                .workflowKey(inst != null ? inst.getWorkflowDefinition().getWorkflowKey() : null)
                .workflowName(inst != null ? inst.getWorkflowDefinition().getName() : null)
                .resourceType(inst != null ? inst.getResourceType() : null)
                .resourceId(inst != null ? inst.getResourceId() : null)
                .currentState(inst != null ? inst.getCurrentState() : null)
                .requesterUserId(inst != null ? inst.getCreatedByUserId() : null)
                .requesterUsername(inst != null ? inst.getCreatedByUsername() : null)
                .transitionId(t.getTransitionId())
                .level(t.getLevel())
                .status(t.getStatus())
                .assignedRole(t.getAssignedRole())
                .assignedTo(t.getAssignedTo())
                .assignedScope(t.getAssignedScope())
                .version(t.getVersion())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .completedBy(t.getCompletedBy())
                .completedByUsername(t.getCompletedByUsername())
                .comment(t.getComment())
                .build();
    }

    private void createAudit(WorkflowInstance inst, String action, String fromState, String toState,
                             Long actorUserId, String actorUsername, Map<String, Object> meta) {
        try {
            String json = objectMapper.writeValueAsString(meta);
            WorkflowAudit audit = WorkflowAudit.builder()
                    .tenantId(inst.getTenantId())
                    .workflowInstance(inst)
                    .action(action)
                    .fromState(fromState)
                    .toState(toState)
                    .actorUserId(actorUserId)
                    .actorUsername(actorUsername)
                    .metadata(json)
                    .build();
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to write workflow audit log", e);
        }
    }

    private String cleanTenant(String tenantId) {
        return (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
    }
}
