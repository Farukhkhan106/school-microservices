# Success Academy ERP SaaS — Phase 4: Generic Workflow & Maker-Checker Engine Architecture

## 1. Executive Summary

Phase 4 introduces a **Generic, Configurable Workflow and Maker-Checker Engine** sitting directly above the Phase 3 RBAC and Scope authorization layer.

The engine establishes a rigorous separation of concerns:
- **Role**: Identity and category of user (e.g. Teacher, Principal, Tenant Admin).
- **Permission**: Atomic business capability (e.g. `leave.submit`, `leave.approve`).
- **Scope**: Data boundary (e.g. `OWN`, `ASSIGNED`, `TENANT`).
- **Feature**: Tenant module entitlement (e.g. `STAFF`, `EXAMINATIONS`).
- **Workflow**: Lifecycle of a business process (definitions, states, transitions).
- **Maker-Checker**: Principle preventing creators/makers from approving their own requests.
- **Approval Task**: Actionable item in an approver's inbox.

---

## 2. Core Execution Pipeline

```text
REQUEST
  ↓
1. Authentication (JWT / Trusted Gateway Header)
  ↓
2. Tenant Isolation (Server-Side X-Tenant-Id)
  ↓
3. Feature Entitlement (FeatureEntitlementService)
  ↓
4. Permission Check (AuthorizationService)
  ↓
5. Scope Check (AuthorizationService - ScopeType hierarchy)
  ↓
6. Workflow State Check (from_state == instance.currentState)
  ↓
7. Maker / Checker Rule (creator != actor for maker-checker transitions)
  ↓
8. Concurrency & Idempotency Check (@Version & Idempotency Key)
  ↓
9. Execute Transition & Complete / Create Approval Tasks
  ↓
10. Persist State & Bump Version
  ↓
11. Append Immutable Audit Record (workflow_audits)
```

---

## 3. Database Schema & Architecture

The workflow engine operates across 7 relational tables in `authdb`:

### 1. `workflow_definitions`
- `id`, `tenant_id` (NULL for system templates; specific tenant ID for custom overrides), `workflow_key`, `name`, `description`, `resource_type`, `feature_key`, `status` (`DRAFT`, `PUBLISHED`, `ARCHIVED`), `version`, `is_system`, `created_by`, `created_at`, `updated_at`.
- **Constraint**: `UNIQUE KEY uk_wf_def (tenant_id, workflow_key, version)`.

### 2. `workflow_states`
- `id`, `workflow_definition_id`, `state_key`, `name`, `state_type` (`INITIAL`, `INTERMEDIATE`, `TERMINAL`), `display_order`, `is_initial`, `is_terminal`.
- **Constraint**: `UNIQUE KEY uk_wfs_def_state (workflow_definition_id, state_key)`.

### 3. `workflow_transitions`
- `id`, `workflow_definition_id`, `from_state`, `to_state`, `action_key`, `name`, `permission_key`, `required_scope`, `approval_required`, `approval_level`, `maker_checker_required`, `requires_comment`, `requires_rejection_reason`, `is_active`.
- **Constraint**: `UNIQUE KEY uk_wft_def_trans (workflow_definition_id, from_state, action_key)`.

### 4. `workflow_instances`
- `id`, `tenant_id`, `workflow_definition_id`, `workflow_version`, `resource_type`, `resource_id`, `current_state`, `created_by_user_id`, `created_by_username`, `version` (Optimistic locking `@Version`), `created_at`, `updated_at`.
- **Constraint**: `UNIQUE KEY uk_wfi_tenant_res (tenant_id, resource_type, resource_id)`.

### 5. `workflow_actions`
- `id`, `workflow_instance_id`, `action_key`, `actor_user_id`, `actor_username`, `from_state`, `to_state`, `comment`, `reason`, `idempotency_key`, `created_at`.
- **Index**: `idx_wfa_idem (idempotency_key)`.

### 6. `approval_tasks`
- `id`, `tenant_id`, `workflow_instance_id`, `transition_id`, `level`, `status` (`PENDING`, `APPROVED`, `REJECTED`, `RETURNED`, `CANCELLED`), `assigned_role`, `assigned_to`, `assigned_scope`, `version` (Optimistic locking `@Version`), `created_at`, `completed_at`, `completed_by`, `completed_by_username`, `comment`.

### 7. `workflow_audits`
- `id`, `tenant_id`, `workflow_instance_id`, `action`, `from_state`, `to_state`, `actor_user_id`, `actor_username`, `metadata` (JSON), `created_at`.

---

## 4. Key Architectural Guarantees

### Immutability of Published Workflow Versions
Once a workflow definition is set to `PUBLISHED`, its states and transitions cannot be directly modified. To change workflow logic:
1. Call `POST /workflow/definitions/{id}/versions`.
2. A new version is created in `DRAFT` status with `version = currentVersion + 1`.
3. Modifications are made to the draft.
4. Call `POST /workflow/definitions/{id}/publish`. The new version is published, automatically archiving older versions for that tenant.
5. In-flight workflow instances continue executing on the immutable version they started with.

### Maker-Checker Separation
When `maker_checker_required` is enabled on a transition (e.g. `leave.approve`, `leave.reject`):
- The server checks whether `instance.createdByUserId == actorUserId`.
- If true, execution is aborted immediately with `403 Forbidden` (`"Maker-checker violation: Workflow creator cannot approve or execute checker action on their own request."`).
- Attempted self-approvals are logged to the security audit trail.

### Optimistic Locking & Concurrency Protection
- Both `workflow_instances` and `approval_tasks` have a `version` column mapped to JPA `@Version`.
- If two approvers attempt to approve simultaneously, only one transaction commits; the other receives `409 Conflict` (`"Concurrent modification conflict: Workflow state has been changed by another user."`).

### Idempotency Protection
- Transition requests accept an `idempotencyKey`.
- If a duplicate request arrives with the same `idempotencyKey`, the engine returns the previously recorded result without re-executing transitions or generating duplicate tasks.

### Mandatory Rejection Reasons & Return Feedback
- Transitions configured with `requires_rejection_reason = true` reject requests lacking a reason with `400 Bad Request`.
- Transitions configured with `requires_comment = true` reject requests lacking comments with `400 Bad Request`.

---

## 5. End-to-End Walkthrough Example

### Scenario: Teacher Submits Leave -> Principal Approves

```text
Step 1: Teacher submits leave application
  POST /workflow/instances
  Payload: {
    "workflowKey": "STAFF_FACULTY_LEAVE",
    "resourceType": "LEAVE",
    "resourceId": "LEAVE_REQ_104",
    "initialComment": "Personal leave for 2 days"
  }
  Result:
    - Instance created in state DRAFT (id = 42, created_by_user_id = 55)

Step 2: Submit and Queue for Approval
  POST /workflow/instances/42/actions { "actionKey": "SUBMIT" }
  POST /workflow/instances/42/actions { "actionKey": "QUEUE_FOR_APPROVAL" }
  Result:
    - State moves to PENDING_APPROVAL
    - ApprovalTask created (id = 88, status = PENDING, level = 1)

Step 3: Teacher attempts self-approval
  POST /workflow/instances/42/actions { "actionKey": "APPROVE" }
  Result:
    - HTTP 403 Forbidden: "Maker-checker violation: Workflow creator cannot approve..."

Step 4: Principal opens Approval Inbox
  GET /workflow/tasks
  Result:
    - Principal sees Task #88 in Inbox
    - (Teacher's own inbox filters out Task #88 due to maker-checker eligibility)

Step 5: Principal approves leave
  POST /workflow/tasks/88/approve { "comment": "Approved by Principal" }
  Result:
    - Instance transitions to APPROVED
    - Task #88 marked as APPROVED
    - Audit log written with Principal's user ID and timestamp
```

---

## 6. How Future Business Modules Integrate

To add a new workflow (e.g. Fees Concession, Student Admission Approval, Purchase Order):
1. Create a `WorkflowDefinition` record (or use API `POST /workflow/definitions`).
2. Define states and transitions specifying `permission_key`, `required_scope`, and `maker_checker_required`.
3. When creating the business entity, call:
   ```java
   workflowEngineService.startWorkflow(tenantId, new StartWorkflowRequest(wfKey, resType, resId, comment));
   ```
4. The business module does NOT need to write custom maker-checker, task queues, or audit infrastructure—the generic engine handles 100% of the lifecycle!
