package com.successacademy.authservice.service;

import com.successacademy.authservice.dto.rbac.*;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.model.entity.Role;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.service.config.ConfigurationAuditService;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.rbac.AuthorizationService;
import com.successacademy.authservice.service.rbac.RbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacSecurityAndScopeTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConfigurationAuditService auditService;

    @Mock
    private FeatureEntitlementService featureEntitlementService;

    @InjectMocks
    private RbacService rbacService;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(rbacService, featureEntitlementService);
    }

    @Test
    @DisplayName("Platform Admin is automatically granted access across tenants")
    void testPlatformAdmin_GrantedAccess() {
        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .userId(1L)
                .username("platform_admin")
                .tenantId("tenant_a")
                .roles(List.of("PLATFORM_ADMIN"))
                .permissions(List.of("tenant.delete", "academic.class.create"))
                .permissionScopes(Map.of("tenant.delete", "ALL", "academic.class.create", "ALL"))
                .build();

        // Stub getEffectiveAuthorization in RbacService via spy or manual mock
        RbacService mockRbac = mock(RbacService.class);
        when(mockRbac.getEffectiveAuthorization(1L, "tenant_a")).thenReturn(authz);
        AuthorizationService authzService = new AuthorizationService(mockRbac, featureEntitlementService);

        boolean allowed = authzService.isAllowed(1L, "tenant_a", "tenant.delete", ScopeType.ALL, null);
        assertTrue(allowed);
    }

    @Test
    @DisplayName("Teacher with ASSIGNED scope is allowed for ASSIGNED requests but denied for TENANT scope")
    void testTeacher_ScopeBoundaryEnforcement() {
        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .userId(3L)
                .username("teacher_alice")
                .tenantId("default")
                .roles(List.of("TEACHER"))
                .permissions(List.of("student.view"))
                .permissionScopes(Map.of("student.view", "ASSIGNED"))
                .build();

        RbacService mockRbac = mock(RbacService.class);
        when(mockRbac.getEffectiveAuthorization(3L, "default")).thenReturn(authz);
        AuthorizationService authzService = new AuthorizationService(mockRbac, featureEntitlementService);

        // Request with ASSIGNED scope -> ALLOW
        assertTrue(authzService.isAllowed(3L, "default", "student.view", ScopeType.ASSIGNED, null));

        // Request with OWN scope -> ALLOW (ASSIGNED covers OWN)
        assertTrue(authzService.isAllowed(3L, "default", "student.view", ScopeType.OWN, null));

        // Request with TENANT scope -> DENIED
        assertFalse(authzService.isAllowed(3L, "default", "student.view", ScopeType.TENANT, null));
    }

    @Test
    @DisplayName("Disabled feature blocks permission even if role has permission granted")
    void testFeatureDisabled_BlocksPermission() {
        EffectiveAuthorizationResponse authz = EffectiveAuthorizationResponse.builder()
                .userId(1L)
                .username("admin")
                .tenantId("default")
                .roles(List.of("TENANT_ADMIN"))
                .permissions(List.of("fees.view"))
                .permissionScopes(Map.of("fees.view", "TENANT"))
                .build();

        RbacService mockRbac = mock(RbacService.class);
        lenient().when(mockRbac.getEffectiveAuthorization(1L, "default")).thenReturn(authz);
        when(featureEntitlementService.getRuntimeEnvironment()).thenReturn("DEV");
        when(featureEntitlementService.isFeatureEnabled("default", "DEV", "FEES")).thenReturn(false);

        AuthorizationService authzService = new AuthorizationService(mockRbac, featureEntitlementService);

        // When FEES is disabled, isAllowed returns false!
        boolean allowed = authzService.isAllowed(1L, "default", "fees.view", ScopeType.TENANT, null);
        assertFalse(allowed);
    }

    @Test
    @DisplayName("Self-privilege escalation is prohibited: User cannot assign roles to themselves")
    void testSelfPrivilegeEscalation_Prohibited() {
        AssignUserRoleRequest req = new AssignUserRoleRequest(2L, true);

        // Actor ID matches target user ID (1L == 1L)
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                rbacService.assignUserRole(1L, req, "default", 1L, "admin", "TENANT_ADMIN"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Self-privilege escalation prohibited"));
    }

    @Test
    @DisplayName("Admin Delegate cannot assign protected system roles or Tenant Administrator")
    void testAdminDelegate_CannotAssignProtectedOrAdminRoles() {
        User target = new User();
        target.setId(5L);
        target.setUsername("sub_admin");

        Role tenantAdminRole = Role.builder()
                .id(2L)
                .roleCode("TENANT_ADMIN")
                .isProtected(false)
                .isSystem(true)
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(tenantAdminRole));

        AssignUserRoleRequest req = new AssignUserRoleRequest(2L, true);

        // Actor is ADMIN_DELEGATE
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                rbacService.assignUserRole(5L, req, "default", 10L, "delegate", "ADMIN_DELEGATE"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Admin Delegate cannot assign"));
    }

    @Test
    @DisplayName("Tenant A administrator cannot access or modify Tenant B's roles")
    void testCrossTenantIsolation() {
        Role tenantBRole = Role.builder()
                .id(99L)
                .tenantId("tenant_b")
                .roleCode("B_CUSTOM")
                .displayName("Role B")
                .build();

        when(roleRepository.findById(99L)).thenReturn(Optional.of(tenantBRole));

        // Tenant A user calls role 99
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                rbacService.getRoleById(99L, "tenant_a"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Role belongs to another school tenant"));
    }

    @Test
    @DisplayName("Custom roles cannot be created with protected platform permissions")
    void testCustomRole_ProtectedPermission_Prohibited() {
        CreateRoleRequest req = CreateRoleRequest.builder()
                .roleCode("DANGEROUS_ROLE")
                .displayName("Dangerous Role")
                .permissions(List.of(new RolePermissionAssignmentDto("tenant.delete", "ALL")))
                .build();

        when(roleRepository.existsByRoleCodeAndTenantId("DANGEROUS_ROLE", "default")).thenReturn(false);
        when(roleRepository.findByRoleCodeAndTenantIdIsNull("DANGEROUS_ROLE")).thenReturn(Optional.empty());

        // Normal tenant administrator creates role
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                rbacService.createCustomRole(req, "default", "admin", "TENANT_ADMIN"));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot assign protected permission"));
    }
}
