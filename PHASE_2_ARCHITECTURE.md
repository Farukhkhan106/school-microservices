# PHASE 2 ARCHITECTURE: TENANT-AWARE ACADEMIC CONFIGURATION

## 1. Executive Overview
Phase 2 establishes the tenant-aware academic configuration foundation for the School ERP / SaaS platform. It strictly separates **Academic Master Configuration** ("what academic structure does this tenant support?") from **Academic Operational Data** ("what actually exists and is offered in an active academic session?").

All academic structures (classes/grades, sections, subjects, and session offerings) are completely dynamic, partitioned by tenant identity, and resolved from database stores without hardcoded class lists or tenant-specific conditional code.

---

## 2. Conceptual Architecture & Data Taxonomy

```
+---------------------------------------------------------------------------------+
|                                     TENANT                                      |
+---------------------------------------------------------------------------------+
             |                                                  |
             v                                                  v
+-----------------------------+               +-----------------------------------+
|    ACADEMIC CONFIGURATION   |               |         ACADEMIC SESSIONS         |
|         (Master Data)       |               |        (Operational State)        |
+-----------------------------+               +-----------------------------------+
| - Academic Classes/Grades   |                                 |
| - Academic Sections         |                                 v
| - Academic Subjects         |               +-----------------------------------+
| - Progression Rules         |               |     SESSION-SPECIFIC OFFERINGS    |
+-----------------------------+               +-----------------------------------+
                                              | - Class-Section Offerings         |
                                              | - Class-Subject Offerings         |
                                              +-----------------------------------+
                                                                |
                                                                v
                                              +-----------------------------------+
                                              |       STUDENT ENROLLMENTS         |
                                              |  (Session -> Class -> Section)    |
                                              +-----------------------------------+
```

---

## 3. Domain Model & Relational Schema

### 3.1 Academic Class / Grade (`academic_classes`)
Represents the grade/ladder levels supported by a school tenant.
- `id` (BIGINT PK)
- `tenant_id` (VARCHAR(50)): Scopes class to a specific tenant.
- `class_code` (VARCHAR(30)): Immutable programmatic key (e.g., `NURSERY`, `1`, `10`, `GRADE_1`).
- `display_name` (VARCHAR(60)): Human-readable label (e.g., `Nursery`, `Class 10`).
- `display_order` (INT): Progression sequence index used by the year transition engine.
- `category` (VARCHAR(30)): Stage tag (`PRE_PRIMARY`, `PRIMARY`, `MIDDLE`, `SECONDARY`, `SENIOR_SECONDARY`).
- `status` (VARCHAR(20)): Lifecycle status (`ACTIVE`, `INACTIVE`, `ARCHIVED`).
- Unique Constraint: `(tenant_id, class_code)`.

### 3.2 Academic Section (`academic_sections`)
Represents sections or division identifiers for the tenant.
- `id` (BIGINT PK)
- `tenant_id` (VARCHAR(50)): Scopes section to the tenant.
- `section_code` (VARCHAR(20)): Section key (e.g., `A`, `B`, `C`, `RED`, `BLUE`).
- `display_name` (VARCHAR(60)): Label (e.g., `Section A`, `Red House`).
- `display_order` (INT): Display sequence.
- `status` (VARCHAR(20)): Lifecycle status (`ACTIVE`, `INACTIVE`, `ARCHIVED`).
- Unique Constraint: `(tenant_id, section_code)`.

### 3.3 Academic Subject (`academic_subjects`)
Represents curriculum subjects configured for the tenant.
- `id` (BIGINT PK)
- `tenant_id` (VARCHAR(50)): Scopes subject to the tenant.
- `subject_code` (VARCHAR(50)): Subject key (e.g., `MATHEMATICS`, `ENGLISH_LIT`).
- `display_name` (VARCHAR(100)): Full name (e.g., `Mathematics`, `English Language & Literature`).
- `subject_type` (VARCHAR(30)): Categorization (`CORE`, `ELECTIVE`, `VOCATIONAL`, `CO_CURRICULAR`).
- `display_order` (INT): Sequence index.
- `status` (VARCHAR(20)): Lifecycle status (`ACTIVE`, `INACTIVE`, `ARCHIVED`).
- Unique Constraint: `(tenant_id, subject_code)`.

### 3.4 Session Class-Section Offerings (`academic_class_sections`)
Operational mapping defining which class and section combinations are actively open in a given academic session.
- `id` (BIGINT PK)
- `tenant_id` (VARCHAR(50))
- `session_id` (BIGINT FK to `academic_sessions`)
- `class_code` (VARCHAR(30))
- `section_code` (VARCHAR(20))
- `capacity` (INT, default 40)
- `room_number` (VARCHAR(50) NULL)
- `status` (VARCHAR(20), default `ACTIVE`)
- Unique Constraint: `(tenant_id, session_id, class_code, section_code)`.

### 3.5 Session Class-Subject Offerings (`academic_class_subjects`)
Operational mapping defining which subjects are offered for a class in a specific academic session.
- `id` (BIGINT PK)
- `tenant_id` (VARCHAR(50))
- `session_id` (BIGINT FK to `academic_sessions`)
- `class_code` (VARCHAR(30))
- `subject_code` (VARCHAR(50))
- `is_mandatory` (BOOLEAN, default TRUE)
- `weekly_periods` (INT, default 5)
- `status` (VARCHAR(20), default `ACTIVE`)
- Unique Constraint: `(tenant_id, session_id, class_code, subject_code)`.

---

## 4. Tenant Isolation & IDOR Protection

1. **Context Resolution**: Server-side request processing extracts `tenant_id` via `TenantContext.getTenantId()`, which is injected by the API Gateway `JwtAuthFilter` from validated JWT claims.
2. **Server-Side Enforcement**: All database queries filter explicitly by `tenant_id`. Mutating or accessing an entity with an ID belonging to a different tenant results in an immediate `404 NOT FOUND` or `403 FORBIDDEN`.
3. **Cross-Tenant Prevention**: Session offering creation strictly validates that both the session and the referenced class/section/subject belong to the same tenant. Cross-tenant references are rejected at the service level.

---

## 5. Dynamic Progression & Session Transition Engine

The promotion rules engine in `AcademicSessionTransitionService` is completely data-driven:
- **Zero Hardcoded Ladders**: The previous hardcoded `NURSERY -> LKG -> 1 ... 12 -> GRADUATE` progression is replaced with dynamic database inspection of `academic_classes` ordered by `display_order`.
- **Tenant-Specific Graduation**:
  - In a Class 1–10 school (e.g., `school_b`), Class 10 is the highest configured class and is automatically suggested for `GRADUATE`.
  - In a Class Nursery–12 school (e.g., `default`), Class 10 promotes to Class 11, and Class 12 is suggested for `GRADUATE`.
  - In a Grade 1–8 school, Grade 8 graduates.
- **Auditing**: Every configuration change is recorded in `academic_audit_logs`.

---

## 6. Historical Data Preservation

- **Operational Data Continuity**: The existing 487 active student enrollments in `2026-2027` (Session 3) and total 490 enrollments remain untouched and fully consistent.
- **Deactivation Semantics**: Classes, sections, and subjects referenced by historical enrollments or assessments cannot be hard-deleted. They use `INACTIVE` or `ARCHIVED` status, preserving historical report cards, transcripts, and marks.

---

## 7. REST APIs Added

### Classes
- `GET /academic-service/academic/classes?activeOnly=false`
- `GET /academic-service/academic/classes/{id}`
- `POST /academic-service/academic/classes` (Admin only)
- `PUT /academic-service/academic/classes/{id}` (Admin only)
- `PATCH /academic-service/academic/classes/{id}/status` (Admin only)

### Sections
- `GET /academic-service/academic/sections?activeOnly=false`
- `GET /academic-service/academic/sections/{id}`
- `POST /academic-service/academic/sections` (Admin only)
- `PUT /academic-service/academic/sections/{id}` (Admin only)
- `PATCH /academic-service/academic/sections/{id}/status` (Admin only)

### Subjects
- `GET /academic-service/academic/subjects?activeOnly=false`
- `GET /academic-service/academic/subjects/{id}`
- `POST /academic-service/academic/subjects` (Admin only)
- `PUT /academic-service/academic/subjects/{id}` (Admin only)
- `PATCH /academic-service/academic/subjects/{id}/status` (Admin only)

### Structure & Offerings
- `GET /academic-service/academic/structure` (Whitelisted/Public or Authenticated)
- `GET /academic-service/academic/sessions/{sessionId}/offerings/sections`
- `POST /academic-service/academic/sessions/{sessionId}/offerings/sections`
- `DELETE /academic-service/academic/sessions/{sessionId}/offerings/sections/{id}`
- `GET /academic-service/academic/sessions/{sessionId}/offerings/subjects`
- `POST /academic-service/academic/sessions/{sessionId}/offerings/subjects`
- `DELETE /academic-service/academic/sessions/{sessionId}/offerings/subjects/{id}`

---

## 8. Frontend Administration UI

- **Route**: `/admin/academic-config` (and alias `/admin/academics/configuration`).
- **Page**: `src/pages/admin/AcademicConfigPage.tsx`
- **Features**:
  - Interactive tabs for Classes, Sections, Subjects, and Session Offerings.
  - Creation & editing modals with form validation.
  - One-click active/inactive toggling.
  - Session offering picker and management.
  - Dynamic navigation link in `DashboardSidebar` and quick access from `AcademicSessionsPage`.

---

## 9. Future Phase Integration Points

- **Phase 3 (Granular Roles & Permissions)**: Will introduce fine-grained permissions (e.g. `academic:class:write`, `academic:offering:manage`) mapped to custom roles.
- **Phase 4 (Workflow / Maker-Checker)**: Will add approval workflows for publishing new class offerings or modifying curriculum subjects mid-session.
- **Phase 5 (Dynamic Dashboard Engine)**: Will display widgets driven by the active tenant's class-section distribution and enrollment counts.
