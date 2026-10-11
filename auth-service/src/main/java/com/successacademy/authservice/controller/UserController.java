package com.successacademy.authservice.controller;

import com.successacademy.authservice.dto.rbac.AssignUserRoleRequest;
import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.dto.rbac.UserRoleDto;
import com.successacademy.authservice.dto.user.*;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.security.SecurityContextUtil;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.rbac.AuthorizationService;
import com.successacademy.authservice.service.user.UserAuditService;
import com.successacademy.authservice.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping({"/users", "/auth/users"})
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final SecurityContextUtil securityContextUtil;
    private final UserAuditService userAuditService;

    // =========================================================================
    // USER SEARCH & LISTING
    // =========================================================================

    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> searchUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.view", ScopeType.TENANT);

        return ResponseEntity.ok(userService.searchUsers(tenantId, query, role, status, userType, page, size, sortBy, sortDir, actorId, actorRole));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        // Can view if actor has user.view permission OR viewing own profile
        if (actorId == null || !actorId.equals(id)) {
            enforcePermission(actorId, tenantId, "user.view", ScopeType.TENANT);
        }

        return ResponseEntity.ok(userService.getUserById(id, tenantId, actorId, actorRole));
    }

    // =========================================================================
    // USER CREATION & PROFILE UPDATES
    // =========================================================================

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody CreateUserRequest body,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.create", ScopeType.TENANT);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(body, tenantId, actorId, actorUsername, actorRole));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest body,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        // Can update if user has user.update permission OR updating own profile
        if (actorId == null || !actorId.equals(id)) {
            enforcePermission(actorId, tenantId, "user.update", ScopeType.TENANT);
        }

        return ResponseEntity.ok(userService.updateUser(id, body, tenantId, actorId, actorUsername, actorRole));
    }

    // =========================================================================
    // LIFECYCLE MANAGEMENT
    // =========================================================================

    @PostMapping("/{id}/activate")
    public ResponseEntity<UserResponseDto> activateUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.activate", ScopeType.TENANT);

        return ResponseEntity.ok(userService.activateUser(id, tenantId, actorId, actorUsername, actorRole));
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserResponseDto> deactivateUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.deactivate", ScopeType.TENANT);

        return ResponseEntity.ok(userService.deactivateUser(id, tenantId, actorId, actorUsername, actorRole));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<UserResponseDto> suspendUser(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.suspend", ScopeType.TENANT);

        return ResponseEntity.ok(userService.suspendUser(id, tenantId, actorId, actorUsername, actorRole));
    }

    // =========================================================================
    // ROLE ASSIGNMENT & PERMISSIONS
    // =========================================================================

    @PostMapping("/{id}/roles")
    public ResponseEntity<UserRoleDto> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignUserRoleRequest body,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.role.assign", ScopeType.TENANT);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.assignRole(id, body, tenantId, actorId, actorUsername, actorRole));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> revokeRole(
            @PathVariable Long id,
            @PathVariable Long roleId,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.role.revoke", ScopeType.TENANT);

        userService.revokeRole(id, roleId, tenantId, actorId, actorUsername, actorRole);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<EffectiveAuthorizationResponse> getEffectivePermissions(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);

        if (actorId == null || !actorId.equals(id)) {
            enforcePermission(actorId, tenantId, "user.view", ScopeType.TENANT);
        }

        return ResponseEntity.ok(userService.getEffectivePermissions(id, tenantId));
    }

    // =========================================================================
    // INVITATION MANAGEMENT
    // =========================================================================

    @PostMapping("/invite")
    public ResponseEntity<InvitationResponseDto> inviteUser(
            @Valid @RequestBody InviteUserRequest body,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.invite", ScopeType.TENANT);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.inviteUser(body, tenantId, actorId, actorUsername, actorRole));
    }

    @PostMapping("/{id}/invite/resend")
    public ResponseEntity<InvitationResponseDto> resendInvitation(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.invite", ScopeType.TENANT);

        return ResponseEntity.ok(userService.resendInvitation(id, tenantId, actorId, actorUsername, actorRole));
    }

    // =========================================================================
    // PASSWORD RESET
    // =========================================================================

    @PostMapping("/{id}/password/reset")
    public ResponseEntity<Void> adminInitiatePasswordReset(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.password.reset", ScopeType.TENANT);

        UserResponseDto user = userService.getUserById(id, tenantId, actorId, actorRole);
        userService.initiatePasswordReset(
                PasswordResetRequest.builder().usernameOrEmail(user.getUsername()).build(),
                tenantId, actorId, actorUsername, actorRole);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/password/force-reset")
    public ResponseEntity<Void> forcePasswordReset(
            @PathVariable Long id,
            @Valid @RequestBody ForcePasswordResetRequest body,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        enforcePermission(actorId, tenantId, "user.password.reset", ScopeType.TENANT);

        userService.forcePasswordReset(id, body, tenantId, actorId, actorUsername, actorRole);
        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<UserAuditLogDto>> getUserAuditLogs(
            @PathVariable Long id,
            HttpServletRequest request) {

        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);

        enforcePermission(actorId, tenantId, "user.view", ScopeType.TENANT);

        return ResponseEntity.ok(userAuditService.getAuditLogsForUser(id));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void enforcePermission(Long actorId, String tenantId, String permissionKey, ScopeType requestedScope) {
        if (actorId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication token required.");
        }
        boolean allowed = authorizationService.isAllowed(actorId, tenantId, permissionKey, requestedScope, null);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: You lack the required permission '" + permissionKey + "' for this operation.");
        }
    }

    private String getEffectiveTenant(HttpServletRequest request) {
        String tenant = securityContextUtil.getTenantId(request);
        return (tenant != null && !tenant.isBlank()) ? tenant : TenantContext.getTenantId();
    }
}
