package com.successacademy.authservice.service;

import com.successacademy.authservice.dto.rbac.AssignUserRoleRequest;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.dto.rbac.UserRoleDto;
import com.successacademy.authservice.dto.user.*;
import com.successacademy.authservice.model.LoginRequest;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.model.entity.PasswordResetToken;
import com.successacademy.authservice.model.entity.Role;
import com.successacademy.authservice.model.entity.UserInvitation;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.security.JwtUtil;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import com.successacademy.authservice.service.rbac.AuthorizationService;
import com.successacademy.authservice.service.rbac.RbacService;
import com.successacademy.authservice.service.user.UserAuditService;
import com.successacademy.authservice.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementSecurityAndLifecycleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserInvitationRepository invitationRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RbacService rbacService;

    @Mock
    private UserAuditService auditService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private FeatureEntitlementService featureEntitlementService;

    @InjectMocks
    private UserServiceImpl userService;

    private AuthServiceImpl authService;
    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, roleRepository, userRoleRepository, passwordEncoder, jwtUtil, rbacService);
        authorizationService = new AuthorizationService(rbacService, featureEntitlementService);
    }

    // =========================================================================
    // 1. ACCOUNT LIFECYCLE & LOGIN ENFORCEMENT
    // =========================================================================

    @Test
    @DisplayName("Active user can login successfully")
    void testActiveUser_CanLogin() {
        User activeUser = User.builder()
                .id(1L)
                .username("active.user")
                .password("$2a$10$encoded")
                .status("ACTIVE")
                .role("TEACHER")
                .tenantId("default")
                .build();

        when(userRepository.findByUsername("active.user")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("pass123", "$2a$10$encoded")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any(), any(), any(), any(), any())).thenReturn("mock.jwt.token");

        var response = authService.login(new LoginRequest("active.user", "pass123"));
        assertNotNull(response);
        assertEquals("active.user", response.getUsername());
        verify(userRepository).save(activeUser);
        assertNotNull(activeUser.getLastLoginAt());
    }

    @Test
    @DisplayName("Inactive user is rejected at login")
    void testInactiveUser_LoginRejected() {
        User inactiveUser = User.builder()
                .id(2L)
                .username("inactive.user")
                .password("$2a$10$encoded")
                .status("INACTIVE")
                .tenantId("default")
                .build();

        when(userRepository.findByUsername("inactive.user")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("pass123", "$2a$10$encoded")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.login(new LoginRequest("inactive.user", "pass123")));

        assertTrue(ex.getMessage().contains("INACTIVE"));
    }

    @Test
    @DisplayName("Suspended user is rejected at login")
    void testSuspendedUser_LoginRejected() {
        User suspendedUser = User.builder()
                .id(3L)
                .username("suspended.user")
                .password("$2a$10$encoded")
                .status("SUSPENDED")
                .tenantId("default")
                .build();

        when(userRepository.findByUsername("suspended.user")).thenReturn(Optional.of(suspendedUser));
        when(passwordEncoder.matches("pass123", "$2a$10$encoded")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                authService.login(new LoginRequest("suspended.user", "pass123")));

        assertTrue(ex.getMessage().contains("SUSPENDED"));
    }

    // =========================================================================
    // 2. LAST TENANT ADMIN PROTECTION
    // =========================================================================

    @Test
    @DisplayName("Last active Tenant Admin cannot be deactivated")
    void testLastTenantAdmin_CannotBeDeactivated() {
        User lastAdmin = User.builder()
                .id(1L)
                .username("admin")
                .status("ACTIVE")
                .role("TENANT_ADMIN")
                .tenantId("default")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(rbacService.getUserRoles(1L, "default")).thenReturn(List.of(
                UserRoleDto.builder().roleCode("TENANT_ADMIN").build()
        ));
        // Other admins count = 0!
        when(userRepository.countOtherActiveUsersWithRole("default", "TENANT_ADMIN", 1L)).thenReturn(0L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.deactivateUser(1L, "default", 999L, "other_actor", "TENANT_ADMIN"));

        assertTrue(ex.getMessage().contains("last active Tenant Administrator"));
        verify(userRepository, never()).save(argThat(u -> "INACTIVE".equals(u.getStatus())));
    }

    @Test
    @DisplayName("Tenant Admin can be deactivated if another active Tenant Admin exists")
    void testTenantAdmin_CanBeDeactivatedIfMultipleExist() {
        User admin1 = User.builder()
                .id(1L)
                .username("admin1")
                .status("ACTIVE")
                .role("TENANT_ADMIN")
                .tenantId("default")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin1));
        when(rbacService.getUserRoles(1L, "default")).thenReturn(List.of(
                UserRoleDto.builder().roleCode("TENANT_ADMIN").build()
        ));
        // Another admin exists!
        when(userRepository.countOtherActiveUsersWithRole("default", "TENANT_ADMIN", 1L)).thenReturn(1L);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = userService.deactivateUser(1L, "default", 999L, "admin2", "TENANT_ADMIN");
        assertEquals("INACTIVE", result.getStatus());
        verify(rbacService).invalidateCache("default", 1L);
    }

    @Test
    @DisplayName("Last Tenant Admin role cannot be revoked")
    void testLastTenantAdmin_RoleCannotBeRevoked() {
        User lastAdmin = User.builder()
                .id(1L)
                .username("admin")
                .status("ACTIVE")
                .role("TENANT_ADMIN")
                .tenantId("default")
                .build();

        Role adminRole = Role.builder().id(2L).roleCode("TENANT_ADMIN").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));
        when(rbacService.getUserRoles(1L, "default")).thenReturn(List.of(
                UserRoleDto.builder().roleCode("TENANT_ADMIN").build()
        ));
        when(userRepository.countOtherActiveUsersWithRole("default", "TENANT_ADMIN", 1L)).thenReturn(0L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.revokeRole(1L, 2L, "default", 999L, "other_actor", "TENANT_ADMIN"));

        assertTrue(ex.getMessage().contains("last active Tenant Administrator"));
        verify(rbacService, never()).revokeUserRole(any(), any(), any(), any(), any(), any());
    }

    // =========================================================================
    // 3. ROLE ELEVATION & PRIVILEGE ESCALATION RULES
    // =========================================================================

    @Test
    @DisplayName("Admin Delegate cannot assign TENANT_ADMIN role")
    void testAdminDelegate_CannotAssignTenantAdmin() {
        Role tenantAdminRole = Role.builder().id(2L).roleCode("TENANT_ADMIN").isProtected(false).build();
        User targetUser = User.builder().id(5L).username("target").tenantId("default").build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(tenantAdminRole));

        AssignUserRoleRequest req = AssignUserRoleRequest.builder().roleId(2L).isPrimary(true).build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.assignRole(5L, req, "default", 10L, "delegate_user", "ADMIN_DELEGATE"));

        assertTrue(ex.getMessage().contains("Admin Delegate cannot assign Tenant Administrator"));
    }

    @Test
    @DisplayName("Tenant Admin cannot assign PLATFORM_ADMIN role")
    void testTenantAdmin_CannotAssignPlatformAdmin() {
        Role platformAdminRole = Role.builder().id(1L).roleCode("PLATFORM_ADMIN").isProtected(true).build();
        User targetUser = User.builder().id(5L).username("target").tenantId("default").build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(targetUser));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(platformAdminRole));

        AssignUserRoleRequest req = AssignUserRoleRequest.builder().roleId(1L).isPrimary(true).build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.assignRole(5L, req, "default", 10L, "tenant_admin", "TENANT_ADMIN"));

        assertTrue(ex.getMessage().contains("protected role"));
    }

    @Test
    @DisplayName("User cannot elevate own role (Self-protection)")
    void testSelfElevation_Prohibited() {
        User selfUser = User.builder().id(10L).username("self_user").tenantId("default").build();
        Role targetRole = Role.builder().id(2L).roleCode("TENANT_ADMIN").isProtected(false).build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(selfUser));
        when(roleRepository.findById(2L)).thenReturn(Optional.of(targetRole));
        when(rbacService.assignUserRole(eq(10L), any(), eq("default"), eq(10L), any(), any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Self-privilege escalation prohibited"));

        AssignUserRoleRequest req = AssignUserRoleRequest.builder().roleId(2L).isPrimary(true).build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.assignRole(10L, req, "default", 10L, "self_user", "STAFF"));

        assertTrue(ex.getMessage().contains("Self-privilege"));
    }

    // =========================================================================
    // 4. TENANT ISOLATION & IDOR PREVENTION
    // =========================================================================

    @Test
    @DisplayName("Tenant A admin cannot view Tenant B user (Tenant Isolation)")
    void testTenantIsolation_CannotViewOtherTenantUser() {
        User tenantBUser = User.builder().id(20L).username("tenant_b_user").tenantId("tenant_b").build();
        when(userRepository.findById(20L)).thenReturn(Optional.of(tenantBUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.getUserById(20L, "tenant_a", 10L, "TENANT_ADMIN"));

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Tenant A admin cannot deactivate Tenant B user (IDOR Protection)")
    void testTenantIsolation_CannotDeactivateOtherTenantUser() {
        User tenantBUser = User.builder().id(20L).username("tenant_b_user").tenantId("tenant_b").build();
        when(userRepository.findById(20L)).thenReturn(Optional.of(tenantBUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.deactivateUser(20L, "tenant_a", 10L, "actor_a", "TENANT_ADMIN"));

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    // =========================================================================
    // 5. INVITATIONS LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Accepting invitation with valid token activates account and sets password")
    void testInvitation_AcceptanceActivatesUser() throws Exception {
        String rawToken = "my_secret_random_token_123456789";
        String tokenHash = sha256(rawToken);

        User invitedUser = User.builder()
                .id(30L)
                .username("invited.user")
                .status("INVITED")
                .tenantId("default")
                .build();

        UserInvitation invitation = UserInvitation.builder()
                .id(1L)
                .user(invitedUser)
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .tenantId("default")
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newEncoded");

        userService.acceptInvitation(new AcceptInvitationRequest(rawToken, "newPassword123"));

        assertEquals("ACTIVE", invitedUser.getStatus());
        assertEquals("$2a$10$newEncoded", invitedUser.getPassword());
        assertEquals("ACCEPTED", invitation.getStatus());
        assertNotNull(invitation.getUsedAt());
        verify(userRepository).save(invitedUser);
        verify(invitationRepository).save(invitation);
        verify(rbacService).invalidateCache("default", 30L);
    }

    @Test
    @DisplayName("Expired invitation token is rejected")
    void testInvitation_ExpiredTokenRejected() throws Exception {
        String rawToken = "expired_token_123456";
        String tokenHash = sha256(rawToken);

        UserInvitation invitation = UserInvitation.builder()
                .id(1L)
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.acceptInvitation(new AcceptInvitationRequest(rawToken, "newPassword123")));

        assertTrue(ex.getMessage().contains("expired"));
        assertEquals("EXPIRED", invitation.getStatus());
    }

    @Test
    @DisplayName("Reused invitation token is rejected")
    void testInvitation_ReusedTokenRejected() throws Exception {
        String rawToken = "already_used_token";
        String tokenHash = sha256(rawToken);

        UserInvitation invitation = UserInvitation.builder()
                .id(1L)
                .tokenHash(tokenHash)
                .status("ACCEPTED")
                .expiresAt(LocalDateTime.now().plusHours(10))
                .build();

        when(invitationRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(invitation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.acceptInvitation(new AcceptInvitationRequest(rawToken, "newPassword123")));

        assertTrue(ex.getMessage().contains("already been used"));
    }

    // =========================================================================
    // 6. PASSWORD RESET LIFECYCLE
    // =========================================================================

    @Test
    @DisplayName("Confirming password reset with valid token updates password")
    void testPasswordReset_ConfirmSuccess() throws Exception {
        String rawToken = "reset_token_xyz987";
        String tokenHash = sha256(rawToken);

        User user = User.builder().id(40L).username("alice").tenantId("default").build();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .tenantId("default")
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("superSecretPass123")).thenReturn("$2a$10$superSecretEncoded");

        userService.confirmPasswordReset(new ConfirmPasswordResetRequest(rawToken, "superSecretPass123"));

        assertEquals("$2a$10$superSecretEncoded", user.getPassword());
        assertEquals("USED", resetToken.getStatus());
        assertNotNull(resetToken.getUsedAt());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
        verify(rbacService).invalidateCache("default", 40L);
    }

    @Test
    @DisplayName("Expired password reset token is rejected")
    void testPasswordReset_ExpiredTokenRejected() throws Exception {
        String rawToken = "expired_reset_token";
        String tokenHash = sha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .tokenHash(tokenHash)
                .status("PENDING")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userService.confirmPasswordReset(new ConfirmPasswordResetRequest(rawToken, "superSecretPass123")));

        assertTrue(ex.getMessage().contains("expired"));
        assertEquals("EXPIRED", resetToken.getStatus());
    }

    // =========================================================================
    // 7. SENSITIVE DATA LEAKAGE PREVENTION
    // =========================================================================

    @Test
    @DisplayName("Password hash and tokens are never returned in UserResponseDto")
    void testUserResponseDto_NeverContainsSensitiveData() {
        User user = User.builder()
                .id(50L)
                .username("john")
                .password("$2a$10$hashedPasswordNeverLeakMe")
                .status("ACTIVE")
                .tenantId("default")
                .build();

        when(userRepository.findById(50L)).thenReturn(Optional.of(user));
        when(rbacService.getUserRoles(50L, "default")).thenReturn(Collections.emptyList());

        UserResponseDto dto = userService.getUserById(50L, "default", 1L, "TENANT_ADMIN");

        assertNotNull(dto);
        assertEquals("john", dto.getUsername());
        // Verify UserResponseDto class has NO password field
        assertFalse(Arrays.stream(UserResponseDto.class.getDeclaredFields())
                .anyMatch(f -> f.getName().toLowerCase().contains("password") && !f.getName().equals("mustChangePassword")));
    }

    // =========================================================================
    // 8. RBAC PERMISSIONS ON USER MANAGEMENT
    // =========================================================================

    @Test
    @DisplayName("Student is denied user.view permission")
    void testStudent_CannotAccessUserManagement() {
        EffectiveAuthorizationResponse studentAuth = EffectiveAuthorizationResponse.builder()
                .userId(100L)
                .roles(List.of("STUDENT"))
                .permissions(List.of("student.own.view"))
                .permissionScopes(Map.of("student.own.view", "OWN"))
                .build();

        when(rbacService.getEffectiveAuthorization(100L, "default")).thenReturn(studentAuth);

        boolean allowed = authorizationService.isAllowed(100L, "default", "user.view", ScopeType.TENANT, null);
        assertFalse(allowed);
    }

    @Test
    @DisplayName("Teacher is denied user.create permission")
    void testTeacher_CannotCreateUsers() {
        EffectiveAuthorizationResponse teacherAuth = EffectiveAuthorizationResponse.builder()
                .userId(200L)
                .roles(List.of("TEACHER"))
                .permissions(List.of("attendance.create", "marks.entry"))
                .permissionScopes(Map.of("attendance.create", "ASSIGNED_CLASS"))
                .build();

        when(rbacService.getEffectiveAuthorization(200L, "default")).thenReturn(teacherAuth);

        boolean allowed = authorizationService.isAllowed(200L, "default", "user.create", ScopeType.TENANT, null);
        assertFalse(allowed);
    }

    @Test
    @DisplayName("Tenant Admin is granted user.create and user.view permissions")
    void testTenantAdmin_GrantedUserManagement() {
        EffectiveAuthorizationResponse adminAuth = EffectiveAuthorizationResponse.builder()
                .userId(1L)
                .roles(List.of("TENANT_ADMIN"))
                .permissions(List.of("user.view", "user.create", "user.role.assign"))
                .permissionScopes(Map.of("user.view", "TENANT", "user.create", "TENANT", "user.role.assign", "TENANT"))
                .build();

        when(rbacService.getEffectiveAuthorization(1L, "default")).thenReturn(adminAuth);

        assertTrue(authorizationService.isAllowed(1L, "default", "user.view", ScopeType.TENANT, null));
        assertTrue(authorizationService.isAllowed(1L, "default", "user.create", ScopeType.TENANT, null));
        assertTrue(authorizationService.isAllowed(1L, "default", "user.role.assign", ScopeType.TENANT, null));
    }

    private String sha256(String raw) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }
}
