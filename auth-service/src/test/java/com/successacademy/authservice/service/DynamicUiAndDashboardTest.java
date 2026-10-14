package com.successacademy.authservice.service;

import com.successacademy.authservice.dto.config.BrandingConfigDto;
import com.successacademy.authservice.dto.config.EffectiveConfigResponse;
import com.successacademy.authservice.dto.config.TenantDto;
import com.successacademy.authservice.dto.dashboard.DashboardWidgetDto;
import com.successacademy.authservice.dto.dashboard.EffectiveDashboardResponse;
import com.successacademy.authservice.dto.dashboard.NavigationItemDto;
import com.successacademy.authservice.dto.dashboard.QuickActionDto;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.dto.rbac.RoleDto;
import com.successacademy.authservice.dto.rbac.RolePermissionAssignmentDto;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.entity.workflow.ApprovalTask;
import com.successacademy.authservice.repository.UserRepository;
import com.successacademy.authservice.repository.workflow.ApprovalTaskRepository;
import com.successacademy.authservice.service.config.ConfigurationResolverService;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.dashboard.DynamicDashboardService;
import com.successacademy.authservice.service.dashboard.DynamicNavigationService;
import com.successacademy.authservice.service.rbac.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicUiAndDashboardTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private FeatureEntitlementService featureEntitlementService;

    @Mock
    private ConfigurationResolverService configResolverService;

    @Mock
    private ApprovalTaskRepository approvalTaskRepository;

    @Mock
    private UserRepository userRepository;

    private DynamicNavigationService navigationService;
    private DynamicDashboardService dashboardService;

    @BeforeEach
    void setUp() {
        navigationService = new DynamicNavigationService(rbacService, featureEntitlementService, approvalTaskRepository);
        dashboardService = new DynamicDashboardService(
                navigationService,
                rbacService,
                featureEntitlementService,
                configResolverService,
                approvalTaskRepository,
                userRepository
        );

        lenient().when(featureEntitlementService.getRuntimeEnvironment()).thenReturn("PROD");
    }

    @Test
    @DisplayName("Navigation: Feature disabled removes navigation item dynamically")
    void resolveNavigation_WhenFeatureDisabled_HidesFeatureItems() {
        // Arrange
        String tenant = "tenant-a";
        when(featureEntitlementService.isFeatureEnabled(eq(tenant), eq("PROD"), anyString()))
                .thenAnswer(inv -> !FeatureKey.FACULTY.equals(inv.getArgument(2)));

        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("ADMIN"))
                .permissions(List.of("faculty.view", "student.view", "attendance.view"))
                .permissionScopes(Map.of("faculty.view", "TENANT"))
                .build();
        when(rbacService.getEffectiveAuthorization(1L, tenant)).thenReturn(authz);

        // Act
        List<NavigationItemDto> nav = navigationService.resolveNavigation(1L, tenant, "ADMIN", null);

        // Assert
        assertTrue(nav.stream().noneMatch(n -> "faculty".equals(n.getKey())), "Faculty item should be hidden when feature is disabled");
        assertTrue(nav.stream().anyMatch(n -> "students".equals(n.getKey())), "Students item should be visible");
    }

    @Test
    @DisplayName("Navigation: Missing permission removes navigation item")
    void resolveNavigation_WhenPermissionMissing_HidesRestrictedItems() {
        // Arrange
        String tenant = "tenant-a";
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        // User only has student.view, lacks faculty.view and fees.view
        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("TEACHER"))
                .permissions(List.of("student.view"))
                .permissionScopes(Map.of("student.view", "ASSIGNED"))
                .build();
        when(rbacService.getEffectiveAuthorization(2L, tenant)).thenReturn(authz);

        // Act
        List<NavigationItemDto> nav = navigationService.resolveNavigation(2L, tenant, "TEACHER", null);

        // Assert
        assertTrue(nav.stream().anyMatch(n -> "teacher.students".equals(n.getKey()) || "students".equals(n.getKey())), "Student item should be visible");
        assertTrue(nav.stream().noneMatch(n -> "faculty".equals(n.getKey())), "Faculty item should be hidden without faculty.view");
        assertTrue(nav.stream().noneMatch(n -> "fees".equals(n.getKey())), "Fees item should be hidden without fees.view");
    }

    @Test
    @DisplayName("Navigation: Empty parent categories are pruned when all children lack permission")
    void resolveNavigation_PrunesEmptyParents() {
        // Arrange
        String tenant = "tenant-a";
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        // User has only student.view, NO academic permissions (no classes, sections, subjects)
        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("OFFICE_STAFF"))
                .permissions(List.of("student.view"))
                .permissionScopes(Map.of("student.view", "TENANT"))
                .build();
        when(rbacService.getEffectiveAuthorization(3L, tenant)).thenReturn(authz);

        // Act
        List<NavigationItemDto> nav = navigationService.resolveNavigation(3L, tenant, "OFFICE_STAFF", null);

        // Assert
        assertTrue(nav.stream().noneMatch(n -> "academics".equals(n.getKey())), "Empty academics group should be pruned");
        assertTrue(nav.stream().noneMatch(n -> "workflows".equals(n.getKey())), "Empty workflows group should be pruned");
    }

    @Test
    @DisplayName("Navigation: Dynamic badges computed from pending workflow tasks")
    void resolveNavigation_ComputesDynamicBadges() {
        // Arrange
        String tenant = "tenant-a";
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        ApprovalTask task = new ApprovalTask();
        task.setId(101L);
        com.successacademy.authservice.model.entity.workflow.WorkflowInstance wi1 = new com.successacademy.authservice.model.entity.workflow.WorkflowInstance();
        wi1.setId(501L);
        wi1.setResourceType("STUDENT_LEAVE_APPROVAL");
        task.setWorkflowInstance(wi1);
        task.setStatus("PENDING");
        when(approvalTaskRepository.findPendingTasksForUser(tenant, 4L, "ADMIN")).thenReturn(List.of(task));

        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("ADMIN"))
                .permissions(List.of("workflow.task.view", "workflow.instance.view"))
                .permissionScopes(Map.of("workflow.task.view", "ASSIGNED"))
                .build();
        when(rbacService.getEffectiveAuthorization(4L, tenant)).thenReturn(authz);

        // Act
        List<NavigationItemDto> nav = navigationService.resolveNavigation(4L, tenant, "ADMIN", null);

        // Assert
        NavigationItemDto wfParent = nav.stream().filter(n -> "workflows".equals(n.getKey())).findFirst().orElse(null);
        assertNotNull(wfParent, "Workflows parent should be present");
        assertEquals("1", wfParent.getBadge(), "Badge should reflect count of 1 pending task");

        NavigationItemDto tasksChild = wfParent.getChildren().stream()
                .filter(c -> "workflow.tasks".equals(c.getKey())).findFirst().orElse(null);
        assertNotNull(tasksChild, "Tasks child item should be present");
        assertEquals("1", tasksChild.getBadge(), "Tasks child badge should reflect 1");
    }

    @Test
    @DisplayName("Dashboard: Admin receives tenant-wide summary KPIs and pending workflow tasks")
    void resolveDashboard_AdminReceivesTenantWideWidgets() {
        // Arrange
        String tenant = "tenant-a";
        EffectiveConfigResponse config = EffectiveConfigResponse.builder()
                .tenant(TenantDto.builder().tenantKey(tenant).displayName("Satwas Public School").build())
                .branding(BrandingConfigDto.builder().schoolName("Satwas Public School").logoUrl("/logo.png").build())
                .business(com.successacademy.authservice.dto.config.BusinessConfigDto.builder()
                        .customRules(Map.of("activeAcademicSession", "2026-2027"))
                        .build())
                .build();
        when(configResolverService.resolveEffectiveConfig(eq(tenant), anyString())).thenReturn(config);
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("ADMIN"))
                .permissions(List.of("student.view", "faculty.view", "fees.view", "workflow.task.view"))
                .permissionScopes(Map.of(
                        "student.view", "TENANT",
                        "faculty.view", "TENANT",
                        "fees.view", "TENANT",
                        "workflow.task.view", "TENANT"
                ))
                .build();
        when(rbacService.getEffectiveAuthorization(1L, tenant)).thenReturn(authz);

        ApprovalTask task = new ApprovalTask();
        task.setId(201L);
        com.successacademy.authservice.model.entity.workflow.WorkflowInstance wi2 = new com.successacademy.authservice.model.entity.workflow.WorkflowInstance();
        wi2.setId(601L);
        wi2.setResourceType("FACULTY_LEAVE_APPROVAL");
        task.setWorkflowInstance(wi2);
        task.setStatus("PENDING");
        task.setCreatedAt(LocalDateTime.now());
        when(approvalTaskRepository.findPendingTasksForUser(tenant, 1L, "ADMIN")).thenReturn(List.of(task));

        // Act
        EffectiveDashboardResponse res = dashboardService.resolveDashboard(1L, "admin_user", tenant, "ADMIN", null);

        // Assert
        assertEquals("Satwas Public School", res.getSchoolName());
        assertEquals("2026-2027", res.getActiveAcademicSession());

        List<DashboardWidgetDto> widgets = res.getWidgets();
        assertTrue(widgets.stream().anyMatch(w -> "student.total".equals(w.getKey())));
        assertTrue(widgets.stream().anyMatch(w -> "faculty.total".equals(w.getKey())));
        assertTrue(widgets.stream().anyMatch(w -> "fees.summary".equals(w.getKey())));
        assertTrue(widgets.stream().anyMatch(w -> "workflow.pendingTasks".equals(w.getKey())));

        DashboardWidgetDto studentWidget = widgets.stream().filter(w -> "student.total".equals(w.getKey())).findFirst().orElseThrow();
        assertEquals(490, studentWidget.getData().get("count"), "Admin TENANT scope should receive total student count 490");
    }

    @Test
    @DisplayName("Dashboard: Teacher with ASSIGNED scope receives assigned classes widget and scoped counts")
    void resolveDashboard_TeacherWithAssignedScopeReceivesClassWidgets() {
        // Arrange
        String tenant = "tenant-a";
        EffectiveConfigResponse config = EffectiveConfigResponse.builder()
                .tenant(TenantDto.builder().tenantKey(tenant).displayName("Satwas Public School").build())
                .business(com.successacademy.authservice.dto.config.BusinessConfigDto.builder()
                        .customRules(Map.of("activeAcademicSession", "2026-2027"))
                        .build())
                .build();
        when(configResolverService.resolveEffectiveConfig(eq(tenant), anyString())).thenReturn(config);
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("TEACHER"))
                .permissions(List.of("student.view", "academic.offering.view", "attendance.view"))
                .permissionScopes(Map.of(
                        "student.view", "ASSIGNED",
                        "academic.offering.view", "ASSIGNED",
                        "attendance.view", "ASSIGNED"
                ))
                .build();
        when(rbacService.getEffectiveAuthorization(2L, tenant)).thenReturn(authz);
        when(approvalTaskRepository.findPendingTasksForUser(anyString(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // Act
        EffectiveDashboardResponse res = dashboardService.resolveDashboard(2L, "teacher_smith", tenant, "TEACHER", null);

        // Assert
        List<DashboardWidgetDto> widgets = res.getWidgets();
        assertTrue(widgets.stream().anyMatch(w -> "teacher.assignedClasses".equals(w.getKey())),
                "Teacher should receive assigned classes widget");

        DashboardWidgetDto studentWidget = widgets.stream().filter(w -> "student.total".equals(w.getKey())).findFirst().orElseThrow();
        assertEquals(64, studentWidget.getData().get("count"), "Teacher ASSIGNED scope should return assigned class student count");
    }

    @Test
    @DisplayName("Dashboard: Student with OWN scope receives personal overview and no unauthorized data")
    void resolveDashboard_StudentWithOwnScopeReceivesPersonalOverview() {
        // Arrange
        String tenant = "tenant-a";
        EffectiveConfigResponse config = EffectiveConfigResponse.builder()
                .tenant(TenantDto.builder().tenantKey(tenant).displayName("Satwas Public School").build())
                .business(com.successacademy.authservice.dto.config.BusinessConfigDto.builder()
                        .customRules(Map.of("activeAcademicSession", "2026-2027"))
                        .build())
                .build();
        when(configResolverService.resolveEffectiveConfig(eq(tenant), anyString())).thenReturn(config);
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("STUDENT"))
                .permissions(List.of("student.view", "attendance.view", "fees.view"))
                .permissionScopes(Map.of(
                        "student.view", "OWN",
                        "attendance.view", "OWN",
                        "fees.view", "OWN"
                ))
                .build();
        when(rbacService.getEffectiveAuthorization(5L, tenant)).thenReturn(authz);
        when(approvalTaskRepository.findPendingTasksForUser(anyString(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // Act
        EffectiveDashboardResponse res = dashboardService.resolveDashboard(5L, "student_john", tenant, "STUDENT", null);

        // Assert
        List<DashboardWidgetDto> widgets = res.getWidgets();
        assertTrue(widgets.stream().anyMatch(w -> "student.myOverview".equals(w.getKey())),
                "Student should receive personal overview widget");
        assertTrue(widgets.stream().noneMatch(w -> "faculty.total".equals(w.getKey())),
                "Student must not receive faculty widget");
        assertTrue(widgets.stream().noneMatch(w -> "teacher.assignedClasses".equals(w.getKey())),
                "Student must not receive teacher class allocations");

        DashboardWidgetDto feeWidget = widgets.stream().filter(w -> "fees.summary".equals(w.getKey())).findFirst().orElseThrow();
        assertEquals("My Fee Status", feeWidget.getTitle(), "OWN scope fee widget should say My Fee Status");
    }

    @Test
    @DisplayName("Dashboard: Quick actions are filtered strictly by permissions")
    void resolveDashboard_QuickActionsFilteredByPermissions() {
        // Arrange
        String tenant = "tenant-a";
        EffectiveConfigResponse config = EffectiveConfigResponse.builder().build();
        when(configResolverService.resolveEffectiveConfig(eq(tenant), anyString())).thenReturn(config);
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        // User with only attendance.create permission
        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .tenantId(tenant)
                .roles(List.of("TEACHER"))
                .permissions(List.of("attendance.create"))
                .permissionScopes(Map.of("attendance.create", "ASSIGNED"))
                .build();
        when(rbacService.getEffectiveAuthorization(6L, tenant)).thenReturn(authz);
        when(approvalTaskRepository.findPendingTasksForUser(anyString(), anyLong(), anyString())).thenReturn(Collections.emptyList());

        // Act
        EffectiveDashboardResponse res = dashboardService.resolveDashboard(6L, "teacher_jane", tenant, "TEACHER", null);

        // Assert
        List<QuickActionDto> actions = res.getQuickActions();
        assertTrue(actions.stream().anyMatch(a -> "action.markAttendance".equals(a.getKey())),
                "Mark Attendance should be present");
        assertTrue(actions.stream().noneMatch(a -> "action.createStudent".equals(a.getKey())),
                "Enroll Student must not be present without student.create");
        assertTrue(actions.stream().noneMatch(a -> "action.collectFee".equals(a.getKey())),
                "Collect Fee must not be present without fees.payment.record");
    }

    @Test
    @DisplayName("Dashboard: Preview mode simulates UI for target role without granting real backend authorization")
    void resolveDashboard_PreviewMode_SimulatesTargetRoleCorrectly() {
        // Arrange
        String tenant = "tenant-a";
        EffectiveConfigResponse config = EffectiveConfigResponse.builder().build();
        when(configResolverService.resolveEffectiveConfig(eq(tenant), anyString())).thenReturn(config);
        when(featureEntitlementService.isFeatureEnabled(anyString(), anyString(), anyString())).thenReturn(true);

        // Available role TEACHER with permissions
        RoleDto teacherRole = RoleDto.builder()
                .roleCode("TEACHER")
                .permissions(List.of(
                        RolePermissionAssignmentDto.builder().permissionKey("attendance.create").defaultScope("ASSIGNED").build(),
                        RolePermissionAssignmentDto.builder().permissionKey("academic.offering.view").defaultScope("ASSIGNED").build()
                ))
                .build();
        when(rbacService.getAvailableRoles(tenant)).thenReturn(List.of(teacherRole));

        // Act: Admin requests preview as TEACHER
        EffectiveDashboardResponse preview = dashboardService.resolveDashboard(1L, "admin_user", tenant, "ADMIN", "TEACHER");

        // Assert
        assertEquals("TEACHER", preview.getUserRole(), "Simulated role in response should be TEACHER");
        assertTrue(preview.getWidgets().stream().anyMatch(w -> "teacher.assignedClasses".equals(w.getKey())),
                "Preview as TEACHER should simulate teacher assigned classes widget");
    }
}
