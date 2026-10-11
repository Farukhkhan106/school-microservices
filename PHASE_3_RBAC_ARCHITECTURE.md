# Phase 3: Granular RBAC, Permissions & Data Scopes Architecture
## Success Academy School ERP / Multi-Tenant SaaS Platform

---

### 1. Architectural Summary & Pipeline
Phase 3 establishes an enterprise-grade, tenant-isolated authorization engine operating strictly along the principle:
```
USER ──► ROLE ──► PERMISSIONS ──► SCOPE ──► FEATURE ENTITLEMENT ──► RESOURCE / ACTION
```

Every incoming request is verified against this multi-dimensional pipeline:
1. **Authenticated Identity**: Gateway validates the signed JWT, extracts `username`, `userId`, `role`, `tenantId`, `studentId`, `teacherId`, and `staffId`, strips any spoofed inbound client headers, and signs trusted internal calls with `X-Internal-Secret`.
2. **Tenant Isolation**: All database operations and role-permission bindings are partitioned by `tenant_id` (with global system roles having `tenant_id = NULL`).
3. **Feature Entitlement**: Before granting action on any feature-gated module (e.g. `FEES`, `ATTENDANCE`, `EXAMINATIONS`, `ACTIVITIES`, `NOTICES`, `EVENTS`, `GALLERY`, `COMMUNICATION`, `STAFF`, `FACULTY`, `ACADEMIC_SESSIONS`), the decision engine verifies whether the tenant has that module enabled.
4. **Permission Grant**: Granular permissions format `<resource>.<action>` (e.g. `academic.class.create`, `student.view`) ensure that broad role checks (`if (role == "ADMIN")`) are eliminated.
5. **Data Boundary / Scope**: A permission is evaluated against the data boundary (`OWN`, `ASSIGNED`, `ASSIGNED_CLASS`, `TENANT`, `ALL`).

---

### 2. Permissions Catalog Model
The permission catalog consists of 71 stable, machine-readable permissions categorized by functional module:
- **Academic Structure (Phase 2 Integration)**:
  - `academic.class.view`, `academic.class.create`, `academic.class.update`, `academic.class.status`
  - `academic.section.view`, `academic.section.create`, `academic.section.update`
  - `academic.subject.view`, `academic.subject.create`, `academic.subject.update`
  - `academic.offering.view`, `academic.offering.manage`
  - `academic.session.view`, `academic.session.manage`
- **Student Roster**: `student.view`, `student.create`, `student.update`, `student.deactivate`
- **Faculty Directory**: `faculty.view`, `faculty.create`, `faculty.update`, `faculty.status`
- **Staff Operations**: `staff.view`, `staff.create`, `staff.update`, `staff.status`
- **Attendance**: `attendance.view`, `attendance.create`, `attendance.update`
- **Fees & Finance**: `fees.view`, `fees.create`, `fees.update`, `fees.payment.record`
- **Examinations**: `exam.view`, `exam.create`, `exam.update`, `exam.marks.entry`, `exam.publish`
- **Classroom Activities**: `activity.view`, `activity.create`, `activity.update`, `activity.grade`, `activity.publish`
- **Leaves & Workflow**: `leave.view`, `leave.create`, `leave.submit`, `leave.approve`, `leave.reject`
- **Communication & Media**: `communication.view`, `communication.send`, `notice.view`, `notice.manage`, `event.view`, `event.manage`, `gallery.view`, `gallery.manage`, `inquiry.view`
- **Reports & Analytics**: `report.view`, `report.export`
- **Security & RBAC**: `role.view`, `role.create`, `role.update`, `user.role.assign`
- **Tenant Configuration**: `configuration.view`, `configuration.update`
- **Protected Platform Permissions**: `tenant.view`, `tenant.create`, `tenant.delete`, `tenant.transferOwnership`, `platform.admin.manage`, `security.role.system.modify`

---

### 3. Role Hierarchy & Boundaries
Roles are pure bundles of permissions and contain zero hardcoded business logic:
1. **PLATFORM_ADMIN** (Global System Role, Protected):
   - Scope: `ALL`
   - Platform-wide cross-tenant administration, tenant provisioning, system role administration.
2. **TENANT_ADMIN** (School Tenant Administrator):
   - Scope: `TENANT`
   - Complete operational control over academic configuration, students, staff, faculty, fees, exams, notices, and tenant custom roles. Zero platform ownership operations.
3. **ADMIN_DELEGATE** (Secondary Administrator):
   - Scope: `TENANT`
   - Configurable delegate administrator for day-to-day operations (students, admissions, scheduling, exam marks, fees, academic structure).
   - Strict security boundary: cannot delete tenant, transfer tenant ownership, modify system roles, or assign platform admin.
4. **PRINCIPAL** (Academic Head):
   - Scope: `TENANT`
   - Oversight of academic structure, class offerings, reports, leave approvals, evaluations, faculty directory.
5. **TEACHER** (Faculty Instructor):
   - `student.view`: Scope `ASSIGNED`
   - `attendance.create` / `attendance.update`: Scope `ASSIGNED_CLASS`
   - `activity.grade` / `activity.publish`: Scope `ASSIGNED_CLASS`
   - `exam.marks.entry`: Scope `ASSIGNED`
   - `academic.class.view` / `academic.section.view` / `academic.subject.view`: Scope `TENANT`
   - `leave.create` / `leave.submit`: Scope `OWN`
6. **STAFF** (Operational Staff):
   - `profile.view`, `attendance.view`, `leave.create`, `leave.submit`, `leave.view`: Scope `OWN`
7. **SUPPORT** (Helpdesk Staff):
   - `inquiry.view`: Scope `TENANT`
   - `leave.create` / `leave.submit`: Scope `OWN` (Approval and rejection are explicitly withheld)
8. **STUDENT** (Student Self-Service):
   - `profile.view`, `attendance.view`, `fees.view`, `result.view`, `activity.view`: Scope `OWN`

---

### 4. Data Scopes & Evaluation Engine
Scopes specify the data boundary within which a granted permission is valid:
- `ALL`: Platform-wide cross-tenant records.
- `TENANT`: All records belonging to the authenticated tenant.
- `ASSIGNED_CLASS`: Records belonging to sections/classes taught by the teacher.
- `ASSIGNED`: Records specifically assigned to the user.
- `OWN`: Records owned by the user (self-service).

Scope hierarchy: `ALL` (weight 5) > `TENANT` (4) > `ASSIGNED_CLASS` (3) > `ASSIGNED` (2) > `OWN` (1).
A user with `TENANT` scope satisfies `ASSIGNED` and `OWN` requests; a user with `ASSIGNED` scope satisfies `OWN` requests but is rejected on `TENANT` requests.

---

### 5. Multi-Tenant Custom Roles
Schools can create tenant-specific custom roles (e.g., `Fee Coordinator`, `Exam Incharge`, `Attendance Head`):
- `POST /auth/roles`: Creates role tagged with the caller's `tenant_id`.
- Tenant custom roles are visible and assignable **only** within that school tenant.
- Tenant A can never view or assign Tenant B custom roles.
- Custom roles are prohibited from including protected platform permissions (`tenant.delete`, `platform.admin.manage`).

---

### 6. Security & Privilege Escalation Protection
- **Self-Escalation Prohibition**: Users are strictly barred from modifying or elevating their own roles (`targetUserId == actorUserId` -> 403 Forbidden).
- **Delegate Escalation Prevention**: An `ADMIN_DELEGATE` or normal `TENANT_ADMIN` cannot assign `PLATFORM_ADMIN` or protected system roles.
- **IDOR Protection**: Direct manipulation of tenant IDs or cross-tenant resource IDs in APIs yields 403/404 with no data leakage.

---

### 7. Performance & In-Memory Caching
- Effective authorization sets (`EffectiveAuthorizationResponse`) are cached in a thread-safe in-memory cache keyed by `${tenantId}:${userId}`.
- Cache entries are automatically invalidated upon:
  - Role permission updates (`updateCustomRole`)
  - Role deletions (`deleteCustomRole`)
  - User role assignment or revocation (`assignUserRole`, `revokeUserRole`)

---

### 8. Frontend Integration
- **Context & Hooks**:
  - `useAuthorization()`: Exposes `hasPermission()`, `hasAnyPermission()`, `hasAllPermissions()`, `hasScope()`, `hasRole()`, `roles`, `permissions`.
  - `usePermission(key, scope?)`: Direct granular check for components.
- **`<PermissionGate permission="..." scope="...">`**: Declarative UI component that hides unauthorized actions and buttons.
- **`<ProtectedRoute requiredRole="..." requiredPermission="..." requiredFeature="...">`**: Centralized route guard that checks Authentication + Feature Entitlement + Permission before loading pages.
- **Dynamic Navigation**: `DashboardSidebar.tsx` evaluates both `isFeatureEnabled(item.featureKey)` and `hasPermission(item.permissionKey)`.
- **Role Management UI**: Aesthetic management page at `/admin/roles` to inspect system roles and create/manage custom tenant roles.
