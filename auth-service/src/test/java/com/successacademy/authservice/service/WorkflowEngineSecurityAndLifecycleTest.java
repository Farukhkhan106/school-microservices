package com.successacademy.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.workflow.*;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.model.entity.workflow.*;
import com.successacademy.authservice.repository.workflow.*;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.rbac.AuthorizationService;
import com.successacademy.authservice.service.workflow.WorkflowDefinitionService;
import com.successacademy.authservice.service.workflow.WorkflowEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineSecurityAndLifecycleTest {

    @Mock
    private WorkflowDefinitionRepository definitionRepository;

    @Mock
    private WorkflowStateRepository stateRepository;

    @Mock
    private WorkflowTransitionRepository transitionRepository;

    @Mock
    private WorkflowInstanceRepository instanceRepository;

    @Mock
    private WorkflowActionRepository actionRepository;

    @Mock
    private ApprovalTaskRepository taskRepository;

    @Mock
    private WorkflowAuditRepository auditRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private FeatureEntitlementService featureEntitlementService;

    private WorkflowDefinitionService definitionService;
    private WorkflowEngineService engineService;
    private ObjectMapper objectMapper;

    private WorkflowDefinition testDefinition;
    private WorkflowState draftState;
    private WorkflowState pendingApprovalState;
    private WorkflowState approvedState;
    private WorkflowState rejectedState;
    private WorkflowTransition submitTransition;
    private WorkflowTransition approveTransition;
    private WorkflowTransition rejectTransition;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        definitionService = new WorkflowDefinitionService(definitionRepository, stateRepository, transitionRepository);
        engineService = new WorkflowEngineService(
                definitionService,
                instanceRepository,
                actionRepository,
                taskRepository,
                auditRepository,
                transitionRepository,
                stateRepository,
                authorizationService,
                featureEntitlementService,
                objectMapper
        );

        testDefinition = WorkflowDefinition.builder()
                .id(100L)
                .tenantId("tenant-alpha")
                .workflowKey("STAFF_FACULTY_LEAVE")
                .name("Leave Workflow")
                .resourceType("LEAVE")
                .featureKey("STAFF")
                .status("PUBLISHED")
                .version(1)
                .build();

        draftState = WorkflowState.builder()
                .id(1L)
                .workflowDefinition(testDefinition)
                .stateKey("DRAFT")
                .name("Draft")
                .isInitial(true)
                .isTerminal(false)
                .build();

        pendingApprovalState = WorkflowState.builder()
                .id(2L)
                .workflowDefinition(testDefinition)
                .stateKey("PENDING_APPROVAL")
                .name("Pending Approval")
                .isInitial(false)
                .isTerminal(false)
                .build();

        approvedState = WorkflowState.builder()
                .id(3L)
                .workflowDefinition(testDefinition)
                .stateKey("APPROVED")
                .name("Approved")
                .isInitial(false)
                .isTerminal(true)
                .build();

        rejectedState = WorkflowState.builder()
                .id(4L)
                .workflowDefinition(testDefinition)
                .stateKey("REJECTED")
                .name("Rejected")
                .isInitial(false)
                .isTerminal(true)
                .build();

        submitTransition = WorkflowTransition.builder()
                .id(10L)
                .workflowDefinition(testDefinition)
                .fromState("DRAFT")
                .toState("PENDING_APPROVAL")
                .actionKey("SUBMIT")
                .name("Submit Leave")
                .permissionKey("leave.submit")
                .requiredScope("OWN")
                .approvalRequired(true)
                .makerCheckerRequired(false)
                .isActive(true)
                .build();

        approveTransition = WorkflowTransition.builder()
                .id(11L)
                .workflowDefinition(testDefinition)
                .fromState("PENDING_APPROVAL")
                .toState("APPROVED")
                .actionKey("APPROVE")
                .name("Approve Leave")
                .permissionKey("leave.approve")
                .requiredScope("TENANT")
                .approvalRequired(true)
                .makerCheckerRequired(true) // MAKER-CHECKER REQUIRED
                .isActive(true)
                .build();

        rejectTransition = WorkflowTransition.builder()
                .id(12L)
                .workflowDefinition(testDefinition)
                .fromState("PENDING_APPROVAL")
                .toState("REJECTED")
                .actionKey("REJECT")
                .name("Reject Leave")
                .permissionKey("leave.reject")
                .requiredScope("TENANT")
                .makerCheckerRequired(true)
                .requiresRejectionReason(true) // REJECTION REASON REQUIRED
                .isActive(true)
                .build();
    }

    // =========================================================================
    // A. WORKFLOW DEFINITIONS & IMMUTABILITY
    // =========================================================================

    @Test
    @DisplayName("A1. Cannot mutate published workflow definition directly (Immutability check)")
    void testCannotMutatePublishedDefinition() {
        when(definitionRepository.findById(100L)).thenReturn(Optional.of(testDefinition));

        UpdateWorkflowDefinitionRequest updateReq = UpdateWorkflowDefinitionRequest.builder()
                .name("Modified Name")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                definitionService.updateDraftDefinition(100L, updateReq, "tenant-alpha", "admin")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("immutable"));
    }

    @Test
    @DisplayName("A2. Create new version from published definition increments version to DRAFT")
    void testCreateNewVersion() {
        when(definitionRepository.findById(100L)).thenReturn(Optional.of(testDefinition));
        when(definitionRepository.findMaxVersion("STAFF_FACULTY_LEAVE", "tenant-alpha")).thenReturn(1);

        WorkflowDefinition newVerDef = WorkflowDefinition.builder()
                .id(101L)
                .tenantId("tenant-alpha")
                .workflowKey("STAFF_FACULTY_LEAVE")
                .name("Leave Workflow")
                .resourceType("LEAVE")
                .status("DRAFT")
                .version(2)
                .build();

        when(definitionRepository.save(any(WorkflowDefinition.class))).thenReturn(newVerDef);
        when(definitionRepository.findById(101L)).thenReturn(Optional.of(newVerDef));

        WorkflowDefinitionDto dto = definitionService.createNextVersion(100L, "tenant-alpha", "admin");

        assertNotNull(dto);
        assertEquals("DRAFT", dto.getStatus());
        assertEquals(2, dto.getVersion());
    }

    // =========================================================================
    // B. MAKER-CHECKER ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("B1. Maker attempting to approve own instance is strictly FORBIDDEN (Maker-Checker Rule)")
    void testMakerSelfApprovalForbidden() {
        Long teacherId = 55L; // Creator / Maker
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(teacherId) // Teacher is the maker
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "PENDING_APPROVAL", "APPROVE"))
                .thenReturn(Optional.of(approveTransition));

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .build();

        // Teacher 55 attempts to approve their own leave request
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, teacherId, "teacher1", "tenant-alpha")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Maker-checker violation"));
        verify(instanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("B2. Eligible Checker (Principal/Admin) can approve Maker's request")
    void testEligibleCheckerCanApprove() {
        Long teacherId = 55L;  // Maker
        Long principalId = 2L; // Checker

        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(teacherId)
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "PENDING_APPROVAL", "APPROVE"))
                .thenReturn(Optional.of(approveTransition));

        // Checker has leave.approve with TENANT scope
        when(authorizationService.isAllowed(eq(principalId), eq("tenant-alpha"), eq("leave.approve"), eq(ScopeType.TENANT), any()))
                .thenReturn(true);

        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(i -> i.getArgument(0));

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .comment("Approved by Principal")
                .build();

        WorkflowInstanceDto result = engineService.executeTransition(500L, req, principalId, "principal", "tenant-alpha");

        assertNotNull(result);
        assertEquals("APPROVED", result.getCurrentState());
        verify(actionRepository).save(any(WorkflowAction.class));
        verify(auditRepository).save(any(WorkflowAudit.class));
    }

    // =========================================================================
    // C. AUTHORIZATION & SCOPE INTEGRATION
    // =========================================================================

    @Test
    @DisplayName("C1. User lacking permission is rejected with 403 Forbidden")
    void testLackingPermissionRejected() {
        Long unauthorizedUserId = 99L;
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(55L)
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "PENDING_APPROVAL", "APPROVE"))
                .thenReturn(Optional.of(approveTransition));

        // Authorization check returns false
        when(authorizationService.isAllowed(eq(unauthorizedUserId), eq("tenant-alpha"), eq("leave.approve"), eq(ScopeType.TENANT), any()))
                .thenReturn(false);

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, unauthorizedUserId, "stranger", "tenant-alpha")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Access denied"));
    }

    @Test
    @DisplayName("C2. Feature disabled for tenant blocks transition execution")
    void testFeatureDisabledBlocksTransition() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(55L)
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        // STAFF feature disabled for this tenant
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(false);

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, 2L, "principal", "tenant-alpha")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("disabled"));
    }

    // =========================================================================
    // D. STATE TRANSITION & REJECTION REASON VALIDATION
    // =========================================================================

    @Test
    @DisplayName("D1. Rejection without required reason is rejected with 400 Bad Request")
    void testRejectionWithoutReasonRejected() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(55L)
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "PENDING_APPROVAL", "REJECT"))
                .thenReturn(Optional.of(rejectTransition));
        when(authorizationService.isAllowed(eq(2L), eq("tenant-alpha"), eq("leave.reject"), eq(ScopeType.TENANT), any()))
                .thenReturn(true);

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("REJECT")
                .reason(null) // MISSING REASON
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, 2L, "principal", "tenant-alpha")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("rejection reason is required"));
    }

    @Test
    @DisplayName("D2. Rejection with valid reason transitions to REJECTED and saves reason")
    void testRejectionWithReasonSucceeds() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(55L)
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "PENDING_APPROVAL", "REJECT"))
                .thenReturn(Optional.of(rejectTransition));
        when(authorizationService.isAllowed(eq(2L), eq("tenant-alpha"), eq("leave.reject"), eq(ScopeType.TENANT), any()))
                .thenReturn(true);
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(i -> i.getArgument(0));

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("REJECT")
                .reason("Insufficient staff coverage for this exam date")
                .build();

        WorkflowInstanceDto result = engineService.executeTransition(500L, req, 2L, "principal", "tenant-alpha");

        assertNotNull(result);
        assertEquals("REJECTED", result.getCurrentState());
    }

    @Test
    @DisplayName("D3. Invalid transition from current state throws 400 Bad Request")
    void testInvalidTransitionThrowsBadRequest() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("DRAFT") // State is DRAFT
                .createdByUserId(55L)
                .version(0L)
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        // APPROVE is not configured from DRAFT
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "DRAFT", "APPROVE"))
                .thenReturn(Optional.empty());

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, 2L, "principal", "tenant-alpha")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid transition"));
    }

    // =========================================================================
    // E. IDEMPOTENCY & CONCURRENCY
    // =========================================================================

    @Test
    @DisplayName("E1. Idempotent action retry returns previous state without re-executing")
    void testIdempotentRetry() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("APPROVED")
                .createdByUserId(55L)
                .version(1L)
                .build();

        WorkflowAction priorAction = WorkflowAction.builder()
                .id(999L)
                .workflowInstance(instance)
                .actionKey("APPROVE")
                .idempotencyKey("idem-unique-1234")
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(actionRepository.findByWorkflowInstanceIdAndIdempotencyKey(500L, "idem-unique-1234"))
                .thenReturn(Optional.of(priorAction));

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .idempotencyKey("idem-unique-1234")
                .build();

        WorkflowInstanceDto result = engineService.executeTransition(500L, req, 2L, "principal", "tenant-alpha");

        assertNotNull(result);
        assertEquals("APPROVED", result.getCurrentState());
        // Verify no duplicate actions or updates saved
        verify(instanceRepository, never()).save(any());
        verify(actionRepository, never()).save(any());
    }

    @Test
    @DisplayName("E2. Concurrency mismatch throws 409 Conflict (Optimistic Locking)")
    void testConcurrencyMismatchThrowsConflict() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(55L)
                .version(3L) // Current version is 3
                .build();

        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .expectedVersion(2L) // Client submitted with stale version 2
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, 2L, "principal", "tenant-alpha")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Concurrent modification conflict"));
    }

    // =========================================================================
    // F. TENANT ISOLATION
    // =========================================================================

    @Test
    @DisplayName("F1. User from Tenant Beta accessing Tenant Alpha workflow returns 404")
    void testCrossTenantAccessBlocked() {
        when(instanceRepository.findByIdAndTenantId(500L, "tenant-beta")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.getInstance(500L, "tenant-beta", 2L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("F2. User from Tenant Beta attempting transition on Tenant Alpha instance returns 404")
    void testCrossTenantTransitionBlocked() {
        when(instanceRepository.findByIdAndTenantId(500L, "tenant-beta")).thenReturn(Optional.empty());

        ExecuteTransitionRequest req = ExecuteTransitionRequest.builder()
                .actionKey("APPROVE")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.executeTransition(500L, req, 2L, "principal", "tenant-beta")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // =========================================================================
    // G. TASK INBOX & AUDIT TRAIL
    // =========================================================================

    @Test
    @DisplayName("G1. Task approval via Inbox delegates to transition and marks task approved")
    void testApproveTaskViaInbox() {
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(55L)
                .version(0L)
                .build();

        ApprovalTask task = ApprovalTask.builder()
                .id(900L)
                .tenantId("tenant-alpha")
                .workflowInstance(instance)
                .level(1)
                .status("PENDING")
                .build();

        when(taskRepository.findByIdAndTenantId(900L, "tenant-alpha")).thenReturn(Optional.of(task));
        when(instanceRepository.findByIdAndTenantId(500L, "tenant-alpha")).thenReturn(Optional.of(instance));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(true);
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndActionKey(100L, "PENDING_APPROVAL", "APPROVE"))
                .thenReturn(Optional.of(approveTransition));
        when(authorizationService.isAllowed(eq(2L), eq("tenant-alpha"), eq("leave.approve"), eq(ScopeType.TENANT), any()))
                .thenReturn(true);
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(i -> i.getArgument(0));

        TaskActionRequest taskReq = TaskActionRequest.builder()
                .comment("Approved via task inbox")
                .build();

        WorkflowInstanceDto res = engineService.approveTask(900L, taskReq, 2L, "principal", "tenant-alpha");

        assertNotNull(res);
        assertEquals("APPROVED", res.getCurrentState());
    }

    @Test
    @DisplayName("G2. Maker is excluded from pending approval tasks when maker-checker is active")
    void testMakerExcludedFromPendingTasks() {
        Long teacherId = 55L;
        WorkflowInstance instance = WorkflowInstance.builder()
                .id(500L)
                .tenantId("tenant-alpha")
                .workflowDefinition(testDefinition)
                .workflowVersion(1)
                .resourceType("LEAVE")
                .resourceId("leave-101")
                .currentState("PENDING_APPROVAL")
                .createdByUserId(teacherId) // Teacher is the maker
                .build();

        ApprovalTask task = ApprovalTask.builder()
                .id(901L)
                .tenantId("tenant-alpha")
                .workflowInstance(instance)
                .level(1)
                .status("PENDING")
                .build();

        when(taskRepository.findPendingTasksForUser("tenant-alpha", teacherId, "TEACHER"))
                .thenReturn(List.of(task));
        when(transitionRepository.findByWorkflowDefinitionIdAndFromStateAndIsActiveTrue(100L, "PENDING_APPROVAL"))
                .thenReturn(List.of(approveTransition));

        List<ApprovalTaskDto> pending = engineService.getPendingTasks("tenant-alpha", teacherId, "TEACHER");

        assertTrue(pending.isEmpty(), "Teacher should not see their own pending task when maker-checker is required");
    }

    @Test
    @DisplayName("G3. Start workflow when feature is disabled throws 403 Forbidden")
    void testStartWorkflowFeatureDisabled() {
        when(definitionRepository.findPublishedDefinitionsForTenant("tenant-alpha", "STAFF_FACULTY_LEAVE"))
                .thenReturn(List.of(testDefinition));
        when(featureEntitlementService.isFeatureEnabled(eq("tenant-alpha"), any(), eq("STAFF"))).thenReturn(false);

        StartWorkflowRequest startReq = StartWorkflowRequest.builder()
                .workflowKey("STAFF_FACULTY_LEAVE")
                .resourceType("LEAVE")
                .resourceId("leave-999")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.startWorkflow("tenant-alpha", startReq, 55L, "teacher")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("disabled"));
    }

    @Test
    @DisplayName("G4. Tampered task ID throws 404 Not Found")
    void testTamperedTaskIdThrowsNotFound() {
        when(taskRepository.findByIdAndTenantId(88888L, "tenant-alpha")).thenReturn(Optional.empty());

        TaskActionRequest req = TaskActionRequest.builder().comment("Approve").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                engineService.approveTask(88888L, req, 2L, "principal", "tenant-alpha")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
