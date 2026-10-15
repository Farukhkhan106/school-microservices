package com.successacademy.authservice.service.dashboard;

import com.successacademy.authservice.dto.config.EffectiveConfigResponse;
import com.successacademy.authservice.dto.dashboard.*;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.model.entity.workflow.ApprovalTask;
import com.successacademy.authservice.model.entity.workflow.WorkflowInstance;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.repository.workflow.ApprovalTaskRepository;
import com.successacademy.authservice.service.config.ConfigurationResolverService;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.rbac.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDashboardService {

    private final DynamicNavigationService navigationService;
    private final RbacService rbacService;
    private final FeatureEntitlementService featureEntitlementService;
    private final ConfigurationResolverService configResolverService;
    private final ApprovalTaskRepository approvalTaskRepository;
    private final UserRepository userRepository;

    public EffectiveDashboardResponse resolveDashboard(Long userId, String username, String tenantId,
                                                       String userRole, String previewRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        String env = featureEntitlementService.getRuntimeEnvironment();

        // 1. Fetch Effective Config for Branding and Academic Session
        EffectiveConfigResponse config = configResolverService.resolveEffectiveConfig(cleanTenant, env);
        String schoolName = (config.getBranding() != null && config.getBranding().getSchoolName() != null)
                ? config.getBranding().getSchoolName()
                : (config.getTenant() != null ? config.getTenant().getDisplayName() : "Success Academy");
        String logoUrl = (config.getBranding() != null) ? config.getBranding().getLogoUrl() : null;
        String activeSession = "2026-2027";
        if (config.getBusiness() != null && config.getBusiness().getCustomRules() != null) {
            Object sess = config.getBusiness().getCustomRules().get("activeAcademicSession");
            if (sess != null) activeSession = sess.toString();
        }

        // 2. Fetch Effective Authorization (or simulated preview)
        EffectiveAuthorizationResponse authz;
        String effectiveRole = (previewRole != null && !previewRole.isBlank()) ? previewRole.toUpperCase() : userRole;
        if (previewRole != null && !previewRole.isBlank()) {
            authz = resolvePreviewAuthorization(cleanTenant, previewRole);
        } else if (userId != null) {
            authz = rbacService.getEffectiveAuthorization(userId, cleanTenant);
        } else {
            authz = EffectiveAuthorizationResponse.builder()
                    .tenantId(cleanTenant)
                    .roles(List.of(effectiveRole != null ? effectiveRole : "ANONYMOUS"))
                    .permissions(Collections.emptyList())
                    .permissionScopes(Collections.emptyMap())
                    .build();
        }

        Set<String> permissions = authz.getPermissions() != null
                ? new HashSet<>(authz.getPermissions())
                : Collections.emptySet();
        Map<String, String> permissionScopes = authz.getPermissionScopes() != null
                ? authz.getPermissionScopes()
                : Collections.emptyMap();

        boolean isSuperAdmin = authz.getRoles() != null &&
                (authz.getRoles().contains("PLATFORM_ADMIN") || authz.getRoles().contains("SUPER_ADMIN"));

        // 3. Navigation Items
        List<NavigationItemDto> navigation = navigationService.resolveNavigation(userId, cleanTenant, userRole, previewRole);

        // 4. Resolve Widgets
        List<DashboardWidgetDto> widgets = resolveAuthorizedWidgets(
                cleanTenant, env, userId, effectiveRole, permissions, permissionScopes, isSuperAdmin, previewRole != null);

        // 5. Resolve Quick Actions
        List<QuickActionDto> quickActions = resolveAuthorizedQuickActions(
                cleanTenant, env, permissions, isSuperAdmin);

        // 6. Badges & Counts
        Map<String, Integer> badges = new HashMap<>();
        int pendingTasksCount = getPendingTasks(cleanTenant, userId, effectiveRole).size();
        if (pendingTasksCount > 0) {
            badges.put("pendingTasks", pendingTasksCount);
        }

        return EffectiveDashboardResponse.builder()
                .tenantKey(cleanTenant)
                .schoolName(schoolName)
                .logoUrl(logoUrl)
                .environment(env)
                .username(username != null ? username : "User")
                .userRole(effectiveRole)
                .activeAcademicSession(activeSession)
                .navigation(navigation)
                .widgets(widgets)
                .quickActions(quickActions)
                .badges(badges)
                .build();
    }

    private List<DashboardWidgetDto> resolveAuthorizedWidgets(
            String tenantId, String env, Long userId, String role,
            Set<String> permissions, Map<String, String> scopes,
            boolean isSuperAdmin, boolean isPreview) {

        List<DashboardWidgetDto> result = new ArrayList<>();
        int order = 1;

        // Widget 1: Total Students (KPI)
        if (canAccessWidget(FeatureKey.ALL_FEATURES.contains("STUDENT") ? "STUDENT" : null, "student.view", tenantId, env, permissions, isSuperAdmin)) {
            String scope = scopes.getOrDefault("student.view", "TENANT");
            Map<String, Object> data = new HashMap<>();
            if ("OWN".equalsIgnoreCase(scope)) {
                data.put("count", 1);
                data.put("subtitle", "Personal Enrollment");
            } else if ("ASSIGNED".equalsIgnoreCase(scope)) {
                data.put("count", 64);
                data.put("subtitle", "Assigned Class Students");
            } else {
                data.put("count", 490);
                data.put("subtitle", "Active Enrolled Students");
            }
            data.put("trend", "+4.2% this session");

            result.add(DashboardWidgetDto.builder()
                    .key("student.total")
                    .title("OWN".equalsIgnoreCase(scope) ? "My Enrollment" : "Total Students")
                    .description("Student count and enrollment status")
                    .componentKey("kpi.stat")
                    .icon("GraduationCap")
                    .displayOrder(order++)
                    .gridWidth(3)
                    .requiredPermission("student.view")
                    .requiredScope(scope)
                    .enabled(true)
                    .dataSource("/api/students/count")
                    .data(data)
                    .build());
        }

        // Widget 2: Total Faculty (KPI)
        if (canAccessWidget(FeatureKey.FACULTY, "faculty.view", tenantId, env, permissions, isSuperAdmin)) {
            Map<String, Object> data = new HashMap<>();
            data.put("count", 24);
            data.put("subtitle", "Teaching Staff");
            data.put("trend", "100% active");

            result.add(DashboardWidgetDto.builder()
                    .key("faculty.total")
                    .title("Teaching Faculty")
                    .description("Active faculty members count")
                    .componentKey("kpi.stat")
                    .icon("Users")
                    .displayOrder(order++)
                    .gridWidth(3)
                    .requiredFeature(FeatureKey.FACULTY)
                    .requiredPermission("faculty.view")
                    .enabled(true)
                    .dataSource("/api/faculty/count")
                    .data(data)
                    .build());
        }

        // Widget 3: Attendance Today (KPI)
        if (canAccessWidget(FeatureKey.ATTENDANCE, "attendance.view", tenantId, env, permissions, isSuperAdmin)) {
            String scope = scopes.getOrDefault("attendance.view", "TENANT");
            Map<String, Object> data = new HashMap<>();
            if ("OWN".equalsIgnoreCase(scope)) {
                data.put("count", "96.4%");
                data.put("subtitle", "My Present Rate");
            } else if ("ASSIGNED".equalsIgnoreCase(scope)) {
                data.put("count", "94.8%");
                data.put("subtitle", "Assigned Section Rate");
            } else {
                data.put("count", "95.2%");
                data.put("subtitle", "Overall Campus Attendance");
            }
            data.put("trend", "Tracked Today");

            result.add(DashboardWidgetDto.builder()
                    .key("attendance.today")
                    .title("Today's Attendance")
                    .description("Daily attendance rate")
                    .componentKey("kpi.stat")
                    .icon("CalendarCheck")
                    .displayOrder(order++)
                    .gridWidth(3)
                    .requiredFeature(FeatureKey.ATTENDANCE)
                    .requiredPermission("attendance.view")
                    .requiredScope(scope)
                    .enabled(true)
                    .dataSource("/api/attendance/rate")
                    .data(data)
                    .build());
        }

        // Widget 4: Fees Summary (KPI)
        if (canAccessWidget(FeatureKey.FEES, "fees.view", tenantId, env, permissions, isSuperAdmin)) {
            String scope = scopes.getOrDefault("fees.view", "TENANT");
            Map<String, Object> data = new HashMap<>();
            if ("OWN".equalsIgnoreCase(scope)) {
                data.put("count", "₹0");
                data.put("subtitle", "All Dues Cleared");
            } else {
                data.put("count", "₹1,85,000");
                data.put("subtitle", "Collected This Month");
            }
            data.put("trend", "Term 1 Session");

            result.add(DashboardWidgetDto.builder()
                    .key("fees.summary")
                    .title("OWN".equalsIgnoreCase(scope) ? "My Fee Status" : "Fee Collections")
                    .description("Financial fee collection status")
                    .componentKey("kpi.stat")
                    .icon("CreditCard")
                    .displayOrder(order++)
                    .gridWidth(3)
                    .requiredFeature(FeatureKey.FEES)
                    .requiredPermission("fees.view")
                    .requiredScope(scope)
                    .enabled(true)
                    .dataSource("/api/fees/summary")
                    .data(data)
                    .build());
        }

        // Widget 5: Workflow Pending Tasks (Generic Approval Engine Widget)
        if (canAccessWidget(null, "workflow.task.view", tenantId, env, permissions, isSuperAdmin)) {
            List<ApprovalTask> pendingTasks = isPreview
                    ? mockPreviewTasks(role)
                    : getPendingTasks(tenantId, userId, role);

            List<Map<String, Object>> taskList = new ArrayList<>();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
            for (ApprovalTask t : pendingTasks) {
                Map<String, Object> item = new HashMap<>();
                item.put("taskId", t.getId());
                String taskType = (t.getWorkflowInstance() != null && t.getWorkflowInstance().getResourceType() != null)
                        ? t.getWorkflowInstance().getResourceType()
                        : "APPROVAL_TASK";
                Long instanceId = t.getWorkflowInstance() != null ? t.getWorkflowInstance().getId() : null;
                item.put("taskType", taskType);
                item.put("workflowInstanceId", instanceId);
                item.put("assignedRole", t.getAssignedRole());
                item.put("status", t.getStatus());
                item.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : "");
                taskList.add(item);
            }

            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("totalPending", pendingTasks.size());
            widgetData.put("tasks", taskList);

            result.add(DashboardWidgetDto.builder()
                    .key("workflow.pendingTasks")
                    .title("Pending Approvals & Tasks")
                    .description("Workflow actions requiring your verification or approval")
                    .componentKey("workflow.pendingTasks")
                    .icon("Inbox")
                    .displayOrder(order++)
                    .gridWidth(6)
                    .requiredPermission("workflow.task.view")
                    .enabled(true)
                    .dataSource("/workflow/tasks/pending")
                    .data(widgetData)
                    .build());
        }

        // Widget 6: Teacher Assigned Classes (Only for users with academic offering view and ASSIGNED scope)
        String offeringScope = scopes.get("academic.offering.view");
        if (canAccessWidget(null, "academic.offering.view", tenantId, env, permissions, isSuperAdmin) &&
                ("ASSIGNED".equalsIgnoreCase(offeringScope) || "TEACHER".equalsIgnoreCase(role))) {

            List<Map<String, Object>> classes = List.of(
                    Map.of("className", "Class 5", "section", "Section A", "subject", "Mathematics", "studentsCount", 32),
                    Map.of("className", "Class 6", "section", "Section B", "subject", "Science", "studentsCount", 32)
            );
            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("assignedClasses", classes);

            result.add(DashboardWidgetDto.builder()
                    .key("teacher.assignedClasses")
                    .title("My Assigned Classes")
                    .description("Active teaching allocations for current session")
                    .componentKey("teacher.assignedClasses")
                    .icon("School")
                    .displayOrder(order++)
                    .gridWidth(6)
                    .requiredPermission("academic.offering.view")
                    .requiredScope("ASSIGNED")
                    .enabled(true)
                    .dataSource("/api/academic/offerings/assigned")
                    .data(widgetData)
                    .build());
        }

        // Widget 7: Student Academic Overview (Only for students with OWN scope)
        String studentScope = scopes.get("student.view");
        if ("OWN".equalsIgnoreCase(studentScope) || "STUDENT".equalsIgnoreCase(role)) {
            Map<String, Object> studentData = new HashMap<>();
            studentData.put("enrolledClass", "Class 10 - Section A");
            studentData.put("attendanceRate", "96.4%");
            studentData.put("feeStatus", "Paid");
            studentData.put("recentExamResult", "A+ (92%) - Mid Term");

            result.add(DashboardWidgetDto.builder()
                    .key("student.myOverview")
                    .title("My Academic Overview")
                    .description("Personal academic progress and attendance status")
                    .componentKey("student.myOverview")
                    .icon("User")
                    .displayOrder(order++)
                    .gridWidth(6)
                    .requiredScope("OWN")
                    .enabled(true)
                    .dataSource("/api/students/me")
                    .data(studentData)
                    .build());
        }

        // Widget 8: Recent Notices (COMMUNICATION)
        if (canAccessWidget(FeatureKey.NOTICES, "notice.view", tenantId, env, permissions, isSuperAdmin)) {
            List<Map<String, Object>> notices = List.of(
                    Map.of("id", 1, "title", "Academic Session 2026-2027 Admissions Open", "date", "Today", "priority", "HIGH"),
                    Map.of("id", 2, "title", "Mid-term Examination Timetable Published", "date", "Yesterday", "priority", "NORMAL")
            );
            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("notices", notices);

            result.add(DashboardWidgetDto.builder()
                    .key("recent.notices")
                    .title("Latest School Notices")
                    .description("Important notices and public announcements")
                    .componentKey("recent.notices")
                    .icon("Bell")
                    .displayOrder(order++)
                    .gridWidth(6)
                    .requiredFeature(FeatureKey.NOTICES)
                    .requiredPermission("notice.view")
                    .enabled(true)
                    .dataSource("/api/notices/recent")
                    .data(widgetData)
                    .build());
        }

        // Widget 9: Upcoming Events (EVENTS)
        if (canAccessWidget(FeatureKey.EVENTS, "event.view", tenantId, env, permissions, isSuperAdmin)) {
            List<Map<String, Object>> events = List.of(
                    Map.of("id", 1, "title", "Annual Sports Meet 2026", "date", "18 Oct 2026", "location", "School Ground"),
                    Map.of("id", 2, "title", "Science Exhibition & Fair", "date", "25 Oct 2026", "location", "Auditorium")
            );
            Map<String, Object> widgetData = new HashMap<>();
            widgetData.put("events", events);

            result.add(DashboardWidgetDto.builder()
                    .key("upcoming.events")
                    .title("Upcoming Events")
                    .description("Calendar events and extracurricular dates")
                    .componentKey("upcoming.events")
                    .icon("Calendar")
                    .displayOrder(order++)
                    .gridWidth(6)
                    .requiredFeature(FeatureKey.EVENTS)
                    .requiredPermission("event.view")
                    .enabled(true)
                    .dataSource("/api/events/upcoming")
                    .data(widgetData)
                    .build());
        }

        return result;
    }

    private List<QuickActionDto> resolveAuthorizedQuickActions(
            String tenantId, String env, Set<String> permissions, boolean isSuperAdmin) {

        List<QuickActionDto> actions = new ArrayList<>();

        if (canAccessAction(null, "student.create", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.createStudent")
                    .label("Enroll Student")
                    .icon("UserPlus")
                    .route("/admin/students?action=create")
                    .requiredPermission("student.create")
                    .color("emerald")
                    .build());
        }

        if (canAccessAction(FeatureKey.ATTENDANCE, "attendance.create", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.markAttendance")
                    .label("Mark Attendance")
                    .icon("CheckSquare")
                    .route("/admin/attendance?action=mark")
                    .requiredFeature(FeatureKey.ATTENDANCE)
                    .requiredPermission("attendance.create")
                    .color("blue")
                    .build());
        }

        if (canAccessAction(FeatureKey.FEES, "fees.payment.record", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.collectFee")
                    .label("Collect Fee")
                    .icon("CreditCard")
                    .route("/admin/fees?action=collect")
                    .requiredFeature(FeatureKey.FEES)
                    .requiredPermission("fees.payment.record")
                    .color("violet")
                    .build());
        }

        if (canAccessAction(FeatureKey.NOTICES, "notice.manage", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.createNotice")
                    .label("Post Notice")
                    .icon("Megaphone")
                    .route("/admin/notices?action=create")
                    .requiredFeature(FeatureKey.NOTICES)
                    .requiredPermission("notice.manage")
                    .color("amber")
                    .build());
        }

        if (canAccessAction(FeatureKey.EXAMINATIONS, "exam.marks.entry", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.enterMarks")
                    .label("Enter Marks")
                    .icon("PenTool")
                    .route("/admin/marks")
                    .requiredFeature(FeatureKey.EXAMINATIONS)
                    .requiredPermission("exam.marks.entry")
                    .color("indigo")
                    .build());
        }

        if (canAccessAction(null, "leave.submit", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.submitLeave")
                    .label("Submit Leave")
                    .icon("FileText")
                    .route("/admin/leave?action=submit")
                    .requiredPermission("leave.submit")
                    .color("rose")
                    .build());
        }

        if (canAccessAction(null, "workflow.definition.manage", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.newWorkflow")
                    .label("Configure Workflow")
                    .icon("GitBranch")
                    .route("/admin/workflows/definitions")
                    .requiredPermission("workflow.definition.manage")
                    .color("cyan")
                    .build());
        }

        if (canAccessAction(null, "configuration.view", tenantId, env, permissions, isSuperAdmin)) {
            actions.add(QuickActionDto.builder()
                    .key("action.settings")
                    .label("School Settings")
                    .icon("Settings")
                    .route("/admin/configuration")
                    .requiredPermission("configuration.view")
                    .color("slate")
                    .build());
        }

        return actions;
    }

    private boolean canAccessWidget(String requiredFeature, String requiredPermission,
                                    String tenantId, String env, Set<String> permissions, boolean isSuperAdmin) {
        if (requiredFeature != null && !requiredFeature.isBlank()) {
            boolean enabled = featureEntitlementService.isFeatureEnabled(tenantId, env, requiredFeature);
            if (!enabled) return false;
        }
        if (isSuperAdmin) return true;
        if (requiredPermission != null && !requiredPermission.isBlank()) {
            return permissions.contains(requiredPermission);
        }
        return true;
    }

    private boolean canAccessAction(String requiredFeature, String requiredPermission,
                                    String tenantId, String env, Set<String> permissions, boolean isSuperAdmin) {
        if (requiredFeature != null && !requiredFeature.isBlank()) {
            boolean enabled = featureEntitlementService.isFeatureEnabled(tenantId, env, requiredFeature);
            if (!enabled) return false;
        }
        if (isSuperAdmin) return true;
        if (requiredPermission != null && !requiredPermission.isBlank()) {
            return permissions.contains(requiredPermission);
        }
        return true;
    }

    private List<ApprovalTask> getPendingTasks(String tenantId, Long userId, String role) {
        try {
            return approvalTaskRepository.findPendingTasksForUser(tenantId, userId, role);
        } catch (Exception e) {
            log.debug("Error retrieving pending tasks for user: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ApprovalTask> mockPreviewTasks(String simulatedRole) {
        List<ApprovalTask> list = new ArrayList<>();
        ApprovalTask t = new ApprovalTask();
        t.setId(9991L);
        WorkflowInstance wi = new WorkflowInstance();
        wi.setId(8881L);
        wi.setResourceType("SIMULATED_PREVIEW_APPROVAL");
        t.setWorkflowInstance(wi);
        t.setAssignedRole(simulatedRole);
        t.setStatus("PENDING");
        list.add(t);
        return list;
    }

    private EffectiveAuthorizationResponse resolvePreviewAuthorization(String tenantId, String previewRole) {
        List<String> perms = new ArrayList<>();
        Map<String, String> scopes = new HashMap<>();

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
}
