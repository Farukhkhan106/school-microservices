# PHASE 8 AUDIT — ADMISSIONS, STUDENT LIFECYCLE & PARENT/GUARDIAN MANAGEMENT

**Success Academy School ERP SaaS**  
**Date:** October 2026  
**Architectural Baseline:** Phases 1–7.1 (Merged & Validated on `main`)  
**Target Services:** `student-service`, `academic-service`, `auth-service`, `api-gateway`, `success-academy-frontend`  

---

## 1. EXISTING STUDENT MODEL AUDIT

### 1.1 `student-service` — `com.successacademy.studentservice.model.Student`
- **Location:** [`f:\Success Academy\school-microservices\student-service\src\main\java\com\successacademy\studentservice\model\Student.java`](file:///f:/Success%20Academy/school-microservices/student-service/src/main/java/com/successacademy/studentservice/model/Student.java)
- **Table:** `studentdb.students`
- **Current Fields:**
  - `id`: `BIGINT PRIMARY KEY AUTO_INCREMENT`
  - **Basic Info:** `firstName` (NOT NULL), `lastName` (NOT NULL), `email`, `phone`, `gender`, `dateOfBirth`, `photoUrl`
  - **Academic Placement:** `admissionNo` (UNIQUE), `rollNo`, `admissionDate`, `status` (`Active` / `Inactive`), `className` (`studentClass`), `section`
  - **Address:** `address`, `city`, `state`, `pincode`
  - **Legacy Flat Parent Fields:** `fatherName` (NOT NULL), `fatherPhone`, `fatherOccupation`, `motherName`, `motherPhone`, `motherOccupation`, `guardianName`, `guardianPhone`, `guardianRelation`
- **Identified Deficiencies & Architectural Gaps:**
  1. **No Multi-Tenant Column:** The existing `Student` entity lacks a `tenantId` / `tenantKey` column. In Phase 8, we must add `tenantId` (defaulting to `'default'`) to ensure strict tenant isolation across SaaS tenants.
  2. **Uncontrolled Lifecycle Status:** `status` is currently an unconstrained String (`Active`/`Inactive`). Phase 8 requires formal lifecycle states: `PROSPECT`, `APPLICANT`, `ADMITTED`, `ACTIVE`, `INACTIVE`, `TRANSFERRED`, `WITHDRAWN`, `GRADUATED`, `DECEASED`.
  3. **Flat Guardian Fields:** Storing father, mother, and guardian as flat string columns on `Student` prevents:
     - 1:N guardian relationships (e.g. both parents + local guardian).
     - Guardian login/portal accounts.
     - Granular communication & pickup authorization flags.
  4. **No Direct Admission Link:** No foreign key or reference linking the student back to their origin `admission_applications` record.

---

## 2. EXISTING ENROLLMENT MODEL AUDIT

### 2.1 `academic-service` — `com.successacademy.academicservice.model.StudentEnrollment`
- **Location:** [`f:\Success Academy\school-microservices\academic-service\src\main\java\com\successacademy\academicservice\model\StudentEnrollment.java`](file:///f:/Success%20Academy/school-microservices/academic-service/src/main/java/com/successacademy/academicservice/model/StudentEnrollment.java)
- **Table:** `academicdb.student_enrollments`
- **Current Structure:**
  - `tenantId` (`VARCHAR(50)`, NOT NULL)
  - `sessionId`, `sessionCode` (FK to `academic_sessions`)
  - `studentId`, `studentName`, `admissionNo`
  - `studentClass`, `section`, `rollNo`
  - `status` (`EnrollmentStatus`: `ACTIVE`, `PROMOTED`, `RETAINED`, `GRADUATED`, `TRANSFERRED`, `WITHDRAWN`, `PENDING`)
  - `enrollmentDate`, `withdrawalDate`, `withdrawalReason`, `remarks`
  - Unique Constraint: `uk_enrollment_tenant_session_student (tenant_id, session_id, student_id)`
  - Index: `idx_enrollment_session_class_sec (tenant_id, session_id, student_class, section)`
- **Key Finding:**
  - `academic-service` already possesses an enterprise-grade academic enrollment foundation built during Phase 2.
  - **CRITICAL DIRECTIVE:** Phase 8 must NOT duplicate the `StudentEnrollment` table or service. When an admission is converted to an active student, `student-service` will call `academic-service`'s enrollment API (`POST /academic/sessions/enrollments`) to register the official academic session enrollment.

---

## 3. EXISTING USER & AUTHENTICATION MODEL AUDIT

### 3.1 `auth-service` — `com.successacademy.authservice.model.User`
- **Location:** [`f:\Success Academy\school-microservices\auth-service\src\main\java\com\successacademy\authservice\model\User.java`](file:///f:/Success%20Academy/school-microservices/auth-service/src/main/java/com/successacademy/authservice/model/User.java)
- **Table:** `authdb.users`
- **Current Fields:**
  - `id`, `username`, `password` (BCrypt), `role`, `email`, `phone`, `firstName`, `lastName`, `displayName`
  - `userType`: `PLATFORM_ADMIN`, `TENANT_ADMIN`, `ADMIN_DELEGATE`, `PRINCIPAL`, `TEACHER`, `STAFF`, `SUPPORT`, `STUDENT`
  - Cross-service Foreign Keys: `studentId` (nullable, FK to `student-service`), `teacherId`, `staffId`
  - `tenantId` (`VARCHAR(64)` NOT NULL)
  - `status`: `INVITED`, `ACTIVE`, `INACTIVE`, `SUSPENDED`, `LOCKED`
- **Key Finding:**
  - `User.java` contains `studentId` for student accounts.
  - For parent/guardian portal accounts, we can support `userType = "GUARDIAN"` (or `"PARENT"`) and a `guardianId` reference or role `PARENT`/`GUARDIAN`.
  - **CRITICAL DIRECTIVE:** Never create a separate user table. Parent and student portal credentials will leverage Phase 6 User Lifecycle (`INVITED`, `ACTIVE`, etc.).

---

## 4. EXISTING ADMISSION & INQUIRY FUNCTIONALITY AUDIT

### 4.1 Backend
- `contact-service` manages generic web inquiries (`Contact` table: `name`, `studentName`, `email`, `phone`, `message`, `enquiryType = "General" | "Event Enquiry"`, `status = "Pending"`).
- There is **no dedicated admission application entity** in any microservice.

### 4.2 Frontend
- `AdmissionsPage.tsx` (`src/pages/public/AdmissionsPage.tsx`): A static public information portal displaying steps, eligibility criteria, and required documents, with a button routing to `/contact`.
- `StudentsPage.tsx` (`src/pages/admin/StudentsPage.tsx`): An administrative CRUD page managing students, photo uploads, academic placement, and fee setup links.

---

## 5. EXISTING WORKFLOW & PERMISSION AUDIT

### 5.1 Workflow Engine (Phase 4)
- **Service:** `WorkflowEngineService.java` in `auth-service`.
- **Registered Workflows in `authdb.workflow_definitions`:**
  - `STAFF_FACULTY_LEAVE` (`resourceType: LEAVE`, `featureKey: STAFF`)
  - `ACADEMIC_ASSESSMENT` (`resourceType: ASSESSMENT`, `featureKey: EXAMINATIONS`)
- **Compatibility:**
  - `WorkflowEngineService` is fully generic and handles any business resource (`startWorkflow`, `executeTransition`, `maker/checker`, `approval tasks`, `audit`).
  - Phase 8 will define `STUDENT_ADMISSION` workflow in `authdb.workflow_definitions` (`resourceType: ADMISSION_APPLICATION`, `featureKey: ADMISSIONS`).

### 5.2 Permissions Catalog (Phase 3)
- Existing student permissions in `authdb.permissions`:
  - `student.view`, `student.create`, `student.update`, `student.deactivate`
- Gaps to add for Phase 8:
  - `admission.view`, `admission.create`, `admission.edit`, `admission.submit`, `admission.review`, `admission.verify_documents`, `admission.approve`, `admission.reject`, `admission.waitlist`, `admission.convert`, `admission.cancel`
  - `student.transfer`, `student.withdraw`, `student.graduate`
  - `guardian.view`, `guardian.manage`

---

## 6. PHASE 8 TARGET ARCHITECTURE DESIGN

```
+---------------------------------------------------------------------------------+
|                               API GATEWAY (:9000)                               |
+---------------------------------------------------------------------------------+
       |                                    |                              |
       v                                    v                              v
+-----------------------+     +--------------------------+     +--------------------+
|  student-service      |     |    academic-service      |     |   auth-service     |
|  (:8085, studentdb)   |     |    (:8093, academicdb)   |     |   (:8084, authdb)  |
+-----------------------+     +--------------------------+     +--------------------+
| - Student (enhanced)  |     | - AcademicSession        |     | - WorkflowEngine   |
| - AdmissionApplication|     | - StudentEnrollment      |     | - Users & Invites  |
| - AdmissionDocument   |     |   (Official academic     |     | - FeatureEntitle-  |
| - Guardian            |     |    session registration) |     |   ment (ADMISSIONS)|
| - StudentGuardian     |     +--------------------------+     +--------------------+
| - Lifecycle Service   |
+-----------------------+
```

### 6.1 Admission Application Lifecycle
```
ENQUIRY (Optional Contact)
   ↓
DRAFT (Applicant data captured)
   ↓
SUBMITTED (Ready for admissions office)
   ↓
UNDER_REVIEW (Assigned to admissions staff)
   ↓
DOCUMENTS_REQUIRED ←→ DOCUMENT_VERIFIED
   ↓
APPROVED (Maker-checker enforced, ready for conversion)
   ↓
CONVERTED (Transactional creation of Student + Guardian + Academic Enrollment)
```
*Terminal Alternate States:* `REJECTED`, `WAITLISTED`, `CANCELLED`.

### 6.2 Student Lifecycle States
```
PROSPECT → APPLICANT → ADMITTED → ACTIVE → PROMOTED / RETAINED
                                    ↓
                     TRANSFERRED / WITHDRAWN / GRADUATED
```

### 6.3 Guardian Model
- Entity `Guardian`: `id`, `tenantKey`, `userId` (nullable), `firstName`, `lastName`, `displayName`, `phone`, `alternatePhone`, `email`, `address`, `occupation`, `status`, `createdAt`, `updatedAt`.
- Association `StudentGuardian`: `studentId`, `guardianId`, `relationshipType` (`FATHER`, `MOTHER`, `GUARDIAN`, `GRANDFATHER`, `GRANDMOTHER`, `OTHER`), `isPrimary`, `isEmergencyContact`, `canPickup`, `canReceiveNotifications`, `canAccessPortal`, `status`.

---

## 7. COMPATIBILITY & HISTORICAL DATA PRESERVATION RULES

1. **Zero Hard Deletes:** No Student, Enrollment, Document, or Application record will ever be deleted with SQL `DELETE`. All state changes use soft-deletes or status transitions (`INACTIVE`, `TRANSFERRED`, `WITHDRAWN`, `ARCHIVED`).
2. **Backward Compatibility on Student Entity:** Existing columns (`fatherName`, `fatherPhone`, etc.) will remain populated on conversion to prevent breaking any legacy screens or third-party reports.
3. **Historical Enrollment Immutability:** Historical enrollments in past academic sessions remain strictly immutable. When a student is transferred or withdrawn, their current enrollment status is updated with withdrawal date/reason, leaving marks, attendance, and fee history intact.
4. **Idempotent Conversion:** Invoking conversion on an already-converted application returns the existing Student record without duplicating records or throwing unhandled errors.

---

*End of Audit Report.*
