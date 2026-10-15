package com.successacademy.authservice.service.dashboard;

import com.successacademy.authservice.dto.dashboard.NavigationItemDto;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.repository.workflow.ApprovalTaskRepository;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.rbac.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicNavigationService {

    private final RbacService rbacService;
    private final FeatureEntitlementService featureEntitlementService;
    private final ApprovalTaskRepository approvalTaskRepository;

    /**
     * Resolves effective, hierarchical navigation items for a given user, tenant, and environment.
     * Evaluates Feature Entitlements + Granular Permissions + Scopes + Badges.
     * Empty parent categories are automatically pruned.
     */
    public List<NavigationItemDto> resolveNavigation(Long userId, String tenantId, String userRole, String previewRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        String env = featureEntitlementService.getRuntimeEnvironment();

        // 1. Obtain effective authorization (permissions & scopes)
        EffectiveAuthorizationResponse authz;
        if (previewRole != null && !previewRole.isBlank()) {
            // Preview simulation: look up permissions configured for the preview role
            authz = resolvePreviewAuthorization(cleanTenant, previewRole);
        } else if (userId != null) {
            authz = rbacService.getEffectiveAuthorization(userId, cleanTenant);
        } else {
            authz = EffectiveAuthorizationResponse.builder()
                    .tenantId(cleanTenant)
                    .roles(Collections.emptyList())
                    .permissions(Collections.emptyList())
                    .permissionScopes(Collections.emptyMap())
                    .build();
        }

        Set<String> userPermissions = authz.getPermissions() != null
                ? new HashSet<>(authz.getPermissions())
                : Collections.emptySet();
        List<String> effectiveRoles = authz.getRoles() != null
                ? authz.getRoles()
                : Collections.emptyList();
        boolean isSuperOrPlatformAdmin = effectiveRoles.contains("PLATFORM_ADMIN") || effectiveRoles.contains("SUPER_ADMIN");

        // 2. Compute dynamic badges
        Map<String, String> dynamicBadges = computeDynamicBadges(cleanTenant, userId, previewRole != null ? previewRole : userRole);

        String effectiveRole = (previewRole != null && !previewRole.isBlank()) ? previewRole.toUpperCase() : userRole;
        if ((effectiveRole == null || effectiveRole.isBlank()) && !effectiveRoles.isEmpty()) {
            effectiveRole = effectiveRoles.get(0);
        }
        if (effectiveRole == null) {
            effectiveRole = "ADMIN";
        } else {
            effectiveRole = effectiveRole.toUpperCase();
        }

        // 3. Build role-scoped navigation catalog
        List<NavigationItemDto> catalog;
        if ("STUDENT".equals(effectiveRole)) {
            catalog = buildStudentNavigationCatalog();
        } else if ("TEACHER".equals(effectiveRole)) {
            catalog = buildTeacherNavigationCatalog();
        } else if ("STAFF".equals(effectiveRole)) {
            catalog = buildStaffNavigationCatalog();
        } else {
            catalog = buildSystemNavigationCatalog();
        }

        // 4. Filter items according to Feature Entitlements and Permissions
        List<NavigationItemDto> permittedItems = new ArrayList<>();
        for (NavigationItemDto item : catalog) {
            if (isItemVisible(item, cleanTenant, env, userPermissions, isSuperOrPlatformAdmin)) {
                // Attach dynamic badge if available
                if (dynamicBadges.containsKey(item.getKey())) {
                    item.setBadge(dynamicBadges.get(item.getKey()));
                }
                permittedItems.add(item);
            }
        }

        // 5. Structure into hierarchical tree and prune empty parents
        return assembleHierarchicalNavigation(permittedItems);
    }

    private boolean isItemVisible(NavigationItemDto item, String tenantId, String env,
                                  Set<String> userPermissions, boolean isSuperAdmin) {
        // Platform & Super Admins bypass menu permission checks, but respect feature flags
        if (item.getRequiredFeature() != null && !item.getRequiredFeature().isBlank()) {
            boolean enabled = featureEntitlementService.isFeatureEnabled(tenantId, env, item.getRequiredFeature());
            if (!enabled) {
                return false;
            }
        }

        if (isSuperAdmin) {
            return true;
        }

        // Check required permission
        if (item.getRequiredPermission() != null && !item.getRequiredPermission().isBlank()) {
            return userPermissions.contains(item.getRequiredPermission());
        }

        // Parent container items without permissions default to true; will be pruned later if children are empty
        return true;
    }

    private List<NavigationItemDto> assembleHierarchicalNavigation(List<NavigationItemDto> items) {
        Map<String, NavigationItemDto> parents = new LinkedHashMap<>();
        Map<String, List<NavigationItemDto>> childrenByParent = new LinkedHashMap<>();

        // Separate parents (parentKey == null) from children
        for (NavigationItemDto item : items) {
            if (item.getParentKey() == null || item.getParentKey().isBlank()) {
                parents.put(item.getKey(), item);
            } else {
                childrenByParent.computeIfAbsent(item.getParentKey(), k -> new ArrayList<>()).add(item);
            }
        }

        List<NavigationItemDto> result = new ArrayList<>();
        for (NavigationItemDto parent : parents.values()) {
            List<NavigationItemDto> children = childrenByParent.get(parent.getKey());
            if (children != null && !children.isEmpty()) {
                children.sort(Comparator.comparingInt(c -> c.getDisplayOrder() != null ? c.getDisplayOrder() : 0));
                parent.setChildren(children);
                result.add(parent);
            } else {
                // If it's a leaf node (has route) or child-less item with permission, keep it
                if (parent.getRoute() != null && !parent.getRoute().isBlank()) {
                    parent.setChildren(Collections.emptyList());
                    result.add(parent);
                }
                // Otherwise, it was an empty parent group whose children were all filtered out -> PRUNE!
            }
        }

        result.sort(Comparator.comparingInt(p -> p.getDisplayOrder() != null ? p.getDisplayOrder() : 0));
        return result;
    }

    private Map<String, String> computeDynamicBadges(String tenantId, Long userId, String role) {
        Map<String, String> badges = new HashMap<>();
        try {
            int pendingTasks = approvalTaskRepository.findPendingTasksForUser(tenantId, userId, role).size();
            if (pendingTasks > 0) {
                badges.put("workflow.tasks", String.valueOf(pendingTasks));
                badges.put("workflows", String.valueOf(pendingTasks));
            }
        } catch (Exception e) {
            log.debug("Could not compute pending workflow badges: {}", e.getMessage());
        }
        return badges;
    }

    private EffectiveAuthorizationResponse resolvePreviewAuthorization(String tenantId, String previewRole) {
        // Query permissions configured for this preview role
        List<String> perms = new ArrayList<>();
        Map<String, String> scopes = new HashMap<>();

        // Check if role exists in DB
        rbacService.getAvailableRoles(tenantId).stream()
                .filter(r -> r.getRoleCode().equalsIgnoreCase(previewRole.trim()))
                .findFirst()
                .ifPresent(r -> {
                    if (r.getPermissions() != null) {
                        r.getPermissions().forEach(p -> {
                            perms.add(p.getPermissionKey());
                            scopes.put(p.getPermissionKey(), p.getDefaultScope());
                        });
                    }
                });

        return EffectiveAuthorizationResponse.builder()
                .tenantId(tenantId)
                .roles(List.of(previewRole.toUpperCase()))
                .permissions(perms)
                .permissionScopes(scopes)
                .build();
    }

    /**
     * Master System Navigation Catalog defining all platform capabilities.
     */
    private List<NavigationItemDto> buildSystemNavigationCatalog() {
        List<NavigationItemDto> list = new ArrayList<>();

        // 1. Dashboard
        list.add(NavigationItemDto.builder()
                .id("nav_dashboard")
                .key("dashboard")
                .label("Dashboard")
                .icon("LayoutDashboard")
                .route("/admin/dashboard")
                .section("OVERVIEW")
                .displayOrder(1)
                .build());

        // 1.5 Admissions (Phase 8)
        list.add(NavigationItemDto.builder()
                .id("nav_admissions")
                .key("admissions")
                .label("Admissions")
                .icon("UserPlus")
                .route("/admin/admissions")
                .section("MANAGEMENT")
                .requiredFeature(FeatureKey.ADMISSIONS)
                .requiredPermission("admission.view")
                .displayOrder(5)
                .build());

        // 2. Students
        list.add(NavigationItemDto.builder()
                .id("nav_students")
                .key("students")
                .label("Students")
                .icon("GraduationCap")
                .route("/admin/students")
                .section("MANAGEMENT")
                .requiredPermission("student.view")
                .displayOrder(10)
                .build());

        // 2.5 Guardians (Phase 8)
        list.add(NavigationItemDto.builder()
                .id("nav_guardians")
                .key("guardians")
                .label("Guardians")
                .icon("HeartHandshake")
                .route("/admin/guardians")
                .section("MANAGEMENT")
                .requiredFeature(FeatureKey.GUARDIAN_PORTAL)
                .requiredPermission("guardian.view")
                .displayOrder(15)
                .build());

        // 3. Faculty
        list.add(NavigationItemDto.builder()
                .id("nav_faculty")
                .key("faculty")
                .label("Faculty")
                .icon("Users")
                .route("/admin/faculty")
                .section("MANAGEMENT")
                .requiredFeature(FeatureKey.FACULTY)
                .requiredPermission("faculty.view")
                .displayOrder(20)
                .build());

        // 4. Staff
        list.add(NavigationItemDto.builder()
                .id("nav_staff")
                .key("staff")
                .label("Staff")
                .icon("UserCheck")
                .route("/admin/staff")
                .section("MANAGEMENT")
                .requiredFeature(FeatureKey.STAFF)
                .requiredPermission("staff.view")
                .displayOrder(30)
                .build());

        // 5. Attendance
        list.add(NavigationItemDto.builder()
                .id("nav_attendance")
                .key("attendance")
                .label("Attendance")
                .icon("CalendarCheck")
                .route("/admin/attendance")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ATTENDANCE)
                .requiredPermission("attendance.view")
                .displayOrder(40)
                .build());

        // 6. Fees & Finance
        list.add(NavigationItemDto.builder()
                .id("nav_fees")
                .key("fees")
                .label("Fees & Finance")
                .icon("CreditCard")
                .route("/admin/fees")
                .section("MANAGEMENT")
                .requiredFeature(FeatureKey.FEES)
                .requiredPermission("fees.view")
                .displayOrder(50)
                .build());

        // 7. Academics (Parent)
        list.add(NavigationItemDto.builder()
                .id("nav_academics_group")
                .key("academics")
                .label("Academics")
                .icon("BookOpen")
                .section("ACADEMICS")
                .displayOrder(60)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_acad_classes")
                .key("academic.classes")
                .label("Classes")
                .icon("School")
                .route("/admin/academics/classes")
                .parentKey("academics")
                .requiredPermission("academic.class.view")
                .displayOrder(61)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_acad_sections")
                .key("academic.sections")
                .label("Sections")
                .icon("Layers")
                .route("/admin/academics/sections")
                .parentKey("academics")
                .requiredPermission("academic.section.view")
                .displayOrder(62)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_acad_subjects")
                .key("academic.subjects")
                .label("Subjects")
                .icon("BookOpenCheck")
                .route("/admin/academics/subjects")
                .parentKey("academics")
                .requiredPermission("academic.subject.view")
                .displayOrder(63)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_acad_offerings")
                .key("academic.offerings")
                .label("Class Offerings")
                .icon("CalendarRange")
                .route("/admin/academics/offerings")
                .parentKey("academics")
                .requiredPermission("academic.offering.view")
                .displayOrder(64)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_acad_sessions")
                .key("academic.sessions")
                .label("Academic Sessions")
                .icon("History")
                .route("/admin/academics/sessions")
                .parentKey("academics")
                .requiredFeature(FeatureKey.ACADEMIC_SESSIONS)
                .requiredPermission("academic.session.view")
                .displayOrder(65)
                .build());

        // 8. Examinations
        list.add(NavigationItemDto.builder()
                .id("nav_examinations")
                .key("examinations")
                .label("Examinations")
                .icon("Award")
                .route("/admin/examinations")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.EXAMINATIONS)
                .requiredPermission("exam.view")
                .displayOrder(70)
                .build());

        // 9. Activities
        list.add(NavigationItemDto.builder()
                .id("nav_activities")
                .key("activities")
                .label("Activities")
                .icon("Trophy")
                .route("/admin/activities")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ACTIVITIES)
                .requiredPermission("activity.view")
                .displayOrder(80)
                .build());

        // 10. Timetable
        list.add(NavigationItemDto.builder()
                .id("nav_timetable")
                .key("timetable")
                .label("Timetable")
                .icon("Clock")
                .route("/admin/timetable")
                .section("ACADEMICS")
                .requiredPermission("academic.offering.view")
                .displayOrder(90)
                .build());

        // 11. Workflows (Parent)
        list.add(NavigationItemDto.builder()
                .id("nav_workflows_group")
                .key("workflows")
                .label("Workflows")
                .icon("GitFork")
                .section("OPERATIONS")
                .displayOrder(100)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_wf_tasks")
                .key("workflow.tasks")
                .label("Approval Inbox")
                .icon("Inbox")
                .route("/admin/workflows/tasks")
                .parentKey("workflows")
                .requiredPermission("workflow.task.view")
                .displayOrder(101)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_wf_instances")
                .key("workflow.instances")
                .label("Workflow Tracking")
                .icon("ListOrdered")
                .route("/admin/workflows")
                .parentKey("workflows")
                .requiredPermission("workflow.instance.view")
                .displayOrder(102)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_wf_definitions")
                .key("workflow.definitions")
                .label("Workflow Definitions")
                .icon("Settings2")
                .route("/admin/workflows/definitions")
                .parentKey("workflows")
                .requiredPermission("workflow.definition.view")
                .displayOrder(103)
                .build());

        // 12. Communication (Parent)
        list.add(NavigationItemDto.builder()
                .id("nav_communication_group")
                .key("communication")
                .label("Communication")
                .icon("MessageSquare")
                .section("COMMUNICATION")
                .displayOrder(110)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_comm_notices")
                .key("notices")
                .label("Notices")
                .icon("Bell")
                .route("/admin/notices")
                .parentKey("communication")
                .requiredFeature(FeatureKey.NOTICES)
                .requiredPermission("notice.view")
                .displayOrder(111)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_comm_events")
                .key("events")
                .label("Events")
                .icon("Calendar")
                .route("/admin/events")
                .parentKey("communication")
                .requiredFeature(FeatureKey.EVENTS)
                .requiredPermission("event.view")
                .displayOrder(112)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_comm_gallery")
                .key("gallery")
                .label("Gallery")
                .icon("Image")
                .route("/admin/gallery")
                .parentKey("communication")
                .requiredFeature(FeatureKey.GALLERY)
                .requiredPermission("gallery.view")
                .displayOrder(113)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_comm_inquiries")
                .key("inquiries")
                .label("Inquiries")
                .icon("HelpCircle")
                .route("/admin/inquiries")
                .parentKey("communication")
                .requiredFeature(FeatureKey.COMMUNICATION)
                .requiredPermission("inquiry.view")
                .displayOrder(114)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_comm_messages")
                .key("messages")
                .label("Messages")
                .icon("MessageSquare")
                .route("/admin/messages")
                .parentKey("communication")
                .requiredFeature(FeatureKey.COMMUNICATION)
                .requiredPermission("communication.view")
                .displayOrder(115)
                .build());

        // 13. Reports
        list.add(NavigationItemDto.builder()
                .id("nav_reports")
                .key("reports")
                .label("Reports")
                .icon("BarChart3")
                .route("/admin/reports")
                .section("OPERATIONS")
                .requiredPermission("report.view")
                .displayOrder(120)
                .build());

        // 13.5. User Management
        list.add(NavigationItemDto.builder()
                .id("nav_users")
                .key("users")
                .label("User Management")
                .icon("Users")
                .route("/admin/users")
                .section("SYSTEM")
                .requiredPermission("user.view")
                .displayOrder(128)
                .build());

        // 14. Roles & Security
        list.add(NavigationItemDto.builder()
                .id("nav_roles")
                .key("roles")
                .label("Role Management")
                .icon("ShieldCheck")
                .route("/admin/roles")
                .section("SYSTEM")
                .requiredPermission("role.view")
                .displayOrder(130)
                .build());

        // 15. Configuration
        list.add(NavigationItemDto.builder()
                .id("nav_configuration_group")
                .key("configuration")
                .label("Configuration")
                .icon("Sliders")
                .section("SYSTEM")
                .displayOrder(140)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_cfg_tenant")
                .key("config.tenant")
                .label("Tenant Settings")
                .icon("Cog")
                .route("/admin/configuration")
                .parentKey("configuration")
                .requiredPermission("configuration.view")
                .displayOrder(141)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_cfg_versions")
                .key("config.versions")
                .label("Version Audit")
                .icon("FileClock")
                .route("/admin/configuration/versions")
                .parentKey("configuration")
                .requiredPermission("configuration.view")
                .displayOrder(142)
                .build());

        return list;
    }

    private List<NavigationItemDto> buildStudentNavigationCatalog() {
        List<NavigationItemDto> list = new ArrayList<>();

        list.add(NavigationItemDto.builder()
                .id("nav_student_dashboard")
                .key("dashboard")
                .label("Dashboard")
                .icon("LayoutDashboard")
                .route("/student")
                .section("OVERVIEW")
                .displayOrder(1)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_profile")
                .key("student.profile")
                .label("My Profile")
                .icon("User")
                .route("/student/profile")
                .section("MANAGEMENT")
                .requiredPermission("student.view")
                .displayOrder(10)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_attendance")
                .key("student.attendance")
                .label("Attendance")
                .icon("CalendarCheck")
                .route("/student/attendance")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ATTENDANCE)
                .requiredPermission("attendance.view")
                .displayOrder(20)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_fees")
                .key("student.fees")
                .label("Fee Status")
                .icon("CreditCard")
                .route("/student/fees")
                .section("MANAGEMENT")
                .requiredFeature(FeatureKey.FEES)
                .requiredPermission("fees.view")
                .displayOrder(30)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_academics")
                .key("student.academics")
                .label("Academics")
                .icon("BookOpen")
                .route("/student/academics")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.FACULTY)
                .displayOrder(40)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_results")
                .key("student.results")
                .label("Results & Exams")
                .icon("Award")
                .route("/student/results")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.EXAMINATIONS)
                .requiredPermission("exam.view")
                .displayOrder(50)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_activities")
                .key("student.activities")
                .label("Activities & Homework")
                .icon("Sparkles")
                .route("/student/activities")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ACTIVITIES)
                .requiredPermission("activity.view")
                .displayOrder(60)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_notices")
                .key("student.notices")
                .label("Circulars & Notices")
                .icon("Bell")
                .route("/student/notices")
                .section("COMMUNICATION")
                .requiredFeature(FeatureKey.NOTICES)
                .requiredPermission("notice.view")
                .displayOrder(70)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_messages")
                .key("student.messages")
                .label("Messages")
                .icon("MessageSquare")
                .route("/student/messages")
                .section("COMMUNICATION")
                .requiredFeature(FeatureKey.COMMUNICATION)
                .requiredPermission("communication.view")
                .displayOrder(80)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_student_settings")
                .key("student.settings")
                .label("Settings")
                .icon("Settings")
                .route("/student/settings")
                .section("SYSTEM")
                .displayOrder(90)
                .build());

        return list;
    }

    private List<NavigationItemDto> buildTeacherNavigationCatalog() {
        List<NavigationItemDto> list = new ArrayList<>();

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_dashboard")
                .key("dashboard")
                .label("Dashboard")
                .icon("LayoutDashboard")
                .route("/teacher")
                .section("OVERVIEW")
                .displayOrder(1)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_classes")
                .key("teacher.classes")
                .label("My Classes")
                .icon("School")
                .route("/teacher/classes")
                .section("ACADEMICS")
                .requiredPermission("academic.class.view")
                .displayOrder(10)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_students")
                .key("teacher.students")
                .label("Students")
                .icon("GraduationCap")
                .route("/teacher/students")
                .section("MANAGEMENT")
                .requiredPermission("student.view")
                .displayOrder(20)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_timetable")
                .key("teacher.timetable")
                .label("Timetable")
                .icon("Calendar")
                .route("/teacher/timetable")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.FACULTY)
                .displayOrder(30)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_attendance")
                .key("teacher.attendance")
                .label("Attendance")
                .icon("CalendarCheck")
                .route("/teacher/attendance")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ATTENDANCE)
                .requiredPermission("attendance.view")
                .displayOrder(40)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_activities")
                .key("teacher.activities")
                .label("Activities & Homework")
                .icon("Sparkles")
                .route("/teacher/activities")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ACTIVITIES)
                .requiredPermission("activity.view")
                .displayOrder(50)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_marks")
                .key("teacher.marks")
                .label("Exam Marks Entry")
                .icon("Award")
                .route("/teacher/marks")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.EXAMINATIONS)
                .requiredPermission("exam.marks.entry")
                .displayOrder(60)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_notices")
                .key("teacher.notices")
                .label("School Notices")
                .icon("Bell")
                .route("/teacher/notices")
                .section("COMMUNICATION")
                .requiredFeature(FeatureKey.NOTICES)
                .requiredPermission("notice.view")
                .displayOrder(70)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_messages")
                .key("teacher.messages")
                .label("Messages")
                .icon("MessageSquare")
                .route("/teacher/messages")
                .section("COMMUNICATION")
                .requiredFeature(FeatureKey.COMMUNICATION)
                .requiredPermission("communication.view")
                .displayOrder(80)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_profile")
                .key("teacher.profile")
                .label("My Profile")
                .icon("User")
                .route("/teacher/profile")
                .section("MANAGEMENT")
                .displayOrder(90)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_teacher_settings")
                .key("teacher.settings")
                .label("Settings")
                .icon("Settings")
                .route("/teacher/settings")
                .section("SYSTEM")
                .displayOrder(100)
                .build());

        return list;
    }

    private List<NavigationItemDto> buildStaffNavigationCatalog() {
        List<NavigationItemDto> list = new ArrayList<>();

        list.add(NavigationItemDto.builder()
                .id("nav_staff_dashboard")
                .key("dashboard")
                .label("Dashboard")
                .icon("LayoutDashboard")
                .route("/staff")
                .section("OVERVIEW")
                .displayOrder(1)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_staff_profile")
                .key("staff.profile")
                .label("My Profile")
                .icon("User")
                .route("/staff/profile")
                .section("MANAGEMENT")
                .displayOrder(10)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_staff_attendance")
                .key("staff.attendance")
                .label("My Attendance")
                .icon("CalendarCheck")
                .route("/staff/attendance")
                .section("ACADEMICS")
                .requiredFeature(FeatureKey.ATTENDANCE)
                .displayOrder(20)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_staff_leave")
                .key("staff.leave")
                .label("Leave Management")
                .icon("CalendarOff")
                .route("/staff/leave")
                .section("MANAGEMENT")
                .displayOrder(30)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_staff_notices")
                .key("staff.notices")
                .label("Notices")
                .icon("Bell")
                .route("/staff/notices")
                .section("COMMUNICATION")
                .requiredFeature(FeatureKey.NOTICES)
                .displayOrder(40)
                .build());

        list.add(NavigationItemDto.builder()
                .id("nav_staff_settings")
                .key("staff.settings")
                .label("Settings")
                .icon("Settings")
                .route("/staff/settings")
                .section("SYSTEM")
                .displayOrder(50)
                .build());

        return list;
    }
}
