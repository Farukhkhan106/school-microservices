package com.successacademy.authservice.controller;

import com.successacademy.authservice.dto.rbac.*;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.security.SecurityContextUtil;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.rbac.AuthorizationService;
import com.successacademy.authservice.service.rbac.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class RbacController {

    private final RbacService rbacService;
    private final AuthorizationService authorizationService;
    private final SecurityContextUtil securityContextUtil;

    // =========================================================================
    // ROLES
    // =========================================================================

    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getRoles(HttpServletRequest request) {
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(rbacService.getAvailableRoles(tenantId));
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id, HttpServletRequest request) {
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(rbacService.getRoleById(id, tenantId));
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleDto> createCustomRole(
            @Valid @RequestBody CreateRoleRequest body,
            HttpServletRequest request) {
        requireRoleManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        String role = securityContextUtil.getCurrentRole(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacService.createCustomRole(body, tenantId, username, role));
    }

    @PutMapping("/roles/{id}")
    public ResponseEntity<RoleDto> updateCustomRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest body,
            HttpServletRequest request) {
        requireRoleManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        String role = securityContextUtil.getCurrentRole(request);

        return ResponseEntity.ok(rbacService.updateCustomRole(id, body, tenantId, username, role));
    }

    @DeleteMapping("/roles/{id}")
    public ResponseEntity<Void> deleteCustomRole(
            @PathVariable Long id,
            HttpServletRequest request) {
        requireRoleManager(request);
        String tenantId = getEffectiveTenant(request);
        String username = securityContextUtil.getCurrentUsername(request);
        String role = securityContextUtil.getCurrentRole(request);

        rbacService.deleteCustomRole(id, tenantId, username, role);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // PERMISSIONS CATALOG
    // =========================================================================

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        return ResponseEntity.ok(rbacService.getAllPermissions());
    }

    // =========================================================================
    // CURRENT USER EFFECTIVE PERMISSIONS
    // =========================================================================

    @GetMapping("/me/permissions")
    public ResponseEntity<EffectiveAuthorizationResponse> getCurrentUserPermissions(HttpServletRequest request) {
        Long userId = securityContextUtil.getCurrentUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(rbacService.getEffectiveAuthorization(userId, tenantId));
    }

    // =========================================================================
    // USER ROLES ASSIGNMENT
    // =========================================================================

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<UserRoleDto>> getUserRoles(
            @PathVariable Long userId,
            HttpServletRequest request) {
        String tenantId = getEffectiveTenant(request);
        return ResponseEntity.ok(rbacService.getUserRoles(userId, tenantId));
    }

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<UserRoleDto> assignUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody AssignUserRoleRequest body,
            HttpServletRequest request) {
        requireRoleManager(request);
        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rbacService.assignUserRole(userId, body, tenantId, actorId, actorUsername, actorRole));
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    public ResponseEntity<Void> revokeUserRole(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            HttpServletRequest request) {
        requireRoleManager(request);
        String tenantId = getEffectiveTenant(request);
        Long actorId = securityContextUtil.getCurrentUserId(request);
        String actorUsername = securityContextUtil.getCurrentUsername(request);
        String actorRole = securityContextUtil.getCurrentRole(request);

        rbacService.revokeUserRole(userId, roleId, tenantId, actorId, actorUsername, actorRole);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // AUTHORIZATION DECISION EVALUATOR
    // =========================================================================

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> evaluateAuthorization(
            @RequestBody Map<String, Object> req,
            HttpServletRequest request) {

        Long userId = req.containsKey("userId") ? Long.parseLong(String.valueOf(req.get("userId"))) : securityContextUtil.getCurrentUserId(request);
        String tenantId = req.containsKey("tenantId") ? String.valueOf(req.get("tenantId")) : getEffectiveTenant(request);
        String permissionKey = String.valueOf(req.get("permission"));
        ScopeType requestedScope = req.containsKey("scope") ? ScopeType.valueOf(String.valueOf(req.get("scope")).toUpperCase()) : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> resourceContext = (Map<String, Object>) req.get("context");

        boolean allowed = authorizationService.isAllowed(userId, tenantId, permissionKey, requestedScope, resourceContext);
        return ResponseEntity.ok(Map.of(
                "allowed", allowed,
                "userId", userId != null ? userId : -1,
                "tenantId", tenantId != null ? tenantId : "default",
                "permission", permissionKey
        ));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String getEffectiveTenant(HttpServletRequest request) {
        String tenant = securityContextUtil.getTenantId(request);
        return (tenant != null && !tenant.isBlank()) ? tenant : TenantContext.getTenantId();
    }

    private void requireRoleManager(HttpServletRequest request) {
        String role = securityContextUtil.getCurrentRole(request);
        if ("ADMIN".equalsIgnoreCase(role)
                || "TENANT_ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role)
                || "PLATFORM_ADMIN".equalsIgnoreCase(role)
                || "ADMIN_DELEGATE".equalsIgnoreCase(role)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Role management authorization required.");
    }
}
