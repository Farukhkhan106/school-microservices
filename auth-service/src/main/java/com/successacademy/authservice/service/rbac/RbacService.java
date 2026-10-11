package com.successacademy.authservice.service.rbac;

import com.successacademy.authservice.dto.rbac.*;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.model.User;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.service.config.ConfigurationAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final ConfigurationAuditService auditService;

    // Cache of effective authorization: Key = "tenantId:userId"
    private final Map<String, EffectiveAuthorizationResponse> authorizationCache = new ConcurrentHashMap<>();

    // Protected platform permissions that CANNOT be assigned to custom tenant roles or Admin Delegate
    private static final Set<String> PROTECTED_PERMISSION_KEYS = Set.of(
            "tenant.create",
            "tenant.delete",
            "tenant.transferOwnership",
            "platform.admin.manage",
            "security.role.system.modify"
    );

    // =========================================================================
    // ROLES MANAGEMENT
    // =========================================================================

    public List<RoleDto> getAvailableRoles(String tenantId) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        List<Role> roles = roleRepository.findAvailableRolesForTenant(cleanTenant);
        return roles.stream().map(this::toRoleDto).collect(Collectors.toList());
    }

    public RoleDto getRoleById(Long roleId, String tenantId) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + roleId));

        // Enforce tenant isolation for custom roles
        if (role.getTenantId() != null && !role.getTenantId().equals(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Role belongs to another school tenant.");
        }

        return toRoleDto(role);
    }

    @Transactional
    public RoleDto createCustomRole(CreateRoleRequest req, String tenantId, String actorUsername, String actorRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        String cleanRoleCode = req.getRoleCode().trim().toUpperCase();

        if (roleRepository.existsByRoleCodeAndTenantId(cleanRoleCode, cleanTenant)
                || roleRepository.findByRoleCodeAndTenantIdIsNull(cleanRoleCode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code '" + cleanRoleCode + "' already exists.");
        }

        // Prevent non-platform admin from creating roles with protected permissions
        boolean isPlatformAdmin = "PLATFORM_ADMIN".equalsIgnoreCase(actorRole) || "SUPER_ADMIN".equalsIgnoreCase(actorRole);
        if (req.getPermissions() != null) {
            for (RolePermissionAssignmentDto p : req.getPermissions()) {
                if (PROTECTED_PERMISSION_KEYS.contains(p.getPermissionKey()) && !isPlatformAdmin) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Cannot assign protected permission '" + p.getPermissionKey() + "' to custom role.");
                }
            }
        }

        Role newRole = Role.builder()
                .tenantId(cleanTenant)
                .roleCode(cleanRoleCode)
                .displayName(req.getDisplayName().trim())
                .description(req.getDescription())
                .isSystem(false)
                .isProtected(false)
                .status("ACTIVE")
                .build();

        Role saved = roleRepository.save(newRole);

        if (req.getPermissions() != null && !req.getPermissions().isEmpty()) {
            for (RolePermissionAssignmentDto p : req.getPermissions()) {
                permissionRepository.findByPermissionKey(p.getPermissionKey()).ifPresent(perm -> {
                    String scope = (p.getDefaultScope() != null && !p.getDefaultScope().isBlank())
                            ? p.getDefaultScope().trim().toUpperCase()
                            : "TENANT";
                    RolePermission rp = RolePermission.builder()
                            .role(saved)
                            .permission(perm)
                            .defaultScope(scope)
                            .build();
                    rolePermissionRepository.save(rp);
                });
            }
        }

        invalidateCacheForTenant(cleanTenant);
        auditService.log(cleanTenant, null, actorUsername, actorRole, "CREATE_CUSTOM_ROLE",
                "Created custom role " + cleanRoleCode + " with " + (req.getPermissions() != null ? req.getPermissions().size() : 0) + " permissions");

        return toRoleDto(saved);
    }

    @Transactional
    public RoleDto updateCustomRole(Long roleId, UpdateRoleRequest req, String tenantId, String actorUsername, String actorRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + roleId));

        if (role.isSystem() || role.isProtected()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify protected built-in system role.");
        }

        if (role.getTenantId() != null && !role.getTenantId().equals(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify role belonging to another tenant.");
        }

        boolean isPlatformAdmin = "PLATFORM_ADMIN".equalsIgnoreCase(actorRole) || "SUPER_ADMIN".equalsIgnoreCase(actorRole);

        if (req.getDisplayName() != null && !req.getDisplayName().isBlank()) {
            role.setDisplayName(req.getDisplayName().trim());
        }
        if (req.getDescription() != null) {
            role.setDescription(req.getDescription());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            role.setStatus(req.getStatus().trim().toUpperCase());
        }

        Role saved = roleRepository.save(role);

        if (req.getPermissions() != null) {
            // Validate protected permissions
            for (RolePermissionAssignmentDto p : req.getPermissions()) {
                if (PROTECTED_PERMISSION_KEYS.contains(p.getPermissionKey()) && !isPlatformAdmin) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Cannot assign protected permission '" + p.getPermissionKey() + "' to custom role.");
                }
            }

            rolePermissionRepository.deleteByRoleId(roleId);
            for (RolePermissionAssignmentDto p : req.getPermissions()) {
                permissionRepository.findByPermissionKey(p.getPermissionKey()).ifPresent(perm -> {
                    String scope = (p.getDefaultScope() != null && !p.getDefaultScope().isBlank())
                            ? p.getDefaultScope().trim().toUpperCase()
                            : "TENANT";
                    RolePermission rp = RolePermission.builder()
                            .role(saved)
                            .permission(perm)
                            .defaultScope(scope)
                            .build();
                    rolePermissionRepository.save(rp);
                });
            }
        }

        invalidateCacheForTenant(cleanTenant);
        auditService.log(cleanTenant, null, actorUsername, actorRole, "UPDATE_CUSTOM_ROLE",
                "Updated custom role " + role.getRoleCode());

        return toRoleDto(saved);
    }

    @Transactional
    public void deleteCustomRole(Long roleId, String tenantId, String actorUsername, String actorRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + roleId));

        if (role.isSystem() || role.isProtected()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete protected built-in system role.");
        }

        if (role.getTenantId() != null && !role.getTenantId().equals(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete role belonging to another tenant.");
        }

        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.delete(role);

        invalidateCacheForTenant(cleanTenant);
        auditService.log(cleanTenant, null, actorUsername, actorRole, "DELETE_CUSTOM_ROLE",
                "Deleted custom role " + role.getRoleCode());
    }

    // =========================================================================
    // PERMISSIONS CATALOG
    // =========================================================================

    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll().stream().map(p -> PermissionDto.builder()
                .id(p.getId())
                .permissionKey(p.getPermissionKey())
                .resourceName(p.getResourceName())
                .actionName(p.getActionName())
                .description(p.getDescription())
                .category(p.getCategory())
                .isSystem(p.isSystem())
                .isProtected(p.isProtected())
                .build()).collect(Collectors.toList());
    }

    // =========================================================================
    // USER ROLES & EFFECTIVE AUTHORIZATION
    // =========================================================================

    public EffectiveAuthorizationResponse getEffectiveAuthorization(Long userId, String tenantId) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        String cacheKey = cleanTenant + ":" + userId;

        EffectiveAuthorizationResponse cached = authorizationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + userId));

        List<UserRole> userRoles = userRoleRepository.findEffectiveRolesForUser(userId, cleanTenant);
        List<String> roleCodes = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();

        for (UserRole ur : userRoles) {
            if ("ACTIVE".equalsIgnoreCase(ur.getRole().getStatus())) {
                roleCodes.add(ur.getRole().getRoleCode());
                roleIds.add(ur.getRole().getId());
            }
        }

        // Backward compatibility: if user has no entries in user_roles yet, map user.getRole()
        if (roleCodes.isEmpty() && user.getRole() != null) {
            String legacyCode = user.getRole().toUpperCase();
            if ("ADMIN".equals(legacyCode)) legacyCode = "TENANT_ADMIN";
            final String lookupCode = legacyCode;
            roleRepository.findByRoleCodeAndTenantIdIsNull(lookupCode).ifPresent(r -> {
                roleCodes.add(r.getRoleCode());
                roleIds.add(r.getId());
            });
        }

        // Aggregate permissions and determine widest scope
        Map<String, String> permissionScopeMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<RolePermission> rolePerms = rolePermissionRepository.findByRoleIdIn(roleIds);
            for (RolePermission rp : rolePerms) {
                String pkey = rp.getPermission().getPermissionKey();
                String scope = rp.getDefaultScope();
                if (!permissionScopeMap.containsKey(pkey)) {
                    permissionScopeMap.put(pkey, scope);
                } else {
                    // Combine scopes: widest scope wins (ALL > TENANT > ASSIGNED_CLASS > ASSIGNED > OWN)
                    String existing = permissionScopeMap.get(pkey);
                    permissionScopeMap.put(pkey, chooseWiderScope(existing, scope));
                }
            }
        }

        EffectiveAuthorizationResponse response = EffectiveAuthorizationResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .tenantId(cleanTenant)
                .roles(roleCodes)
                .permissions(new ArrayList<>(permissionScopeMap.keySet()))
                .permissionScopes(permissionScopeMap)
                .build();

        authorizationCache.put(cacheKey, response);
        return response;
    }

    public List<UserRoleDto> getUserRoles(Long userId, String tenantId) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        List<UserRole> list = userRoleRepository.findByUserIdAndTenantId(userId, cleanTenant);
        return list.stream().map(ur -> UserRoleDto.builder()
                .id(ur.getId())
                .userId(ur.getUser().getId())
                .username(ur.getUser().getUsername())
                .roleId(ur.getRole().getId())
                .roleCode(ur.getRole().getRoleCode())
                .roleDisplayName(ur.getRole().getDisplayName())
                .tenantId(ur.getTenantId())
                .isPrimary(ur.isPrimary())
                .assignedAt(ur.getAssignedAt())
                .assignedBy(ur.getAssignedBy())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public UserRoleDto assignUserRole(Long targetUserId, AssignUserRoleRequest req, String tenantId,
                                      Long actorUserId, String actorUsername, String actorRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";

        // 1. Self-escalation protection: Cannot assign roles to yourself
        if (targetUserId.equals(actorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Self-privilege escalation prohibited: Cannot alter your own roles.");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found."));

        Role targetRole = roleRepository.findById(req.getRoleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found with id: " + req.getRoleId()));

        // Tenant isolation: Role must be global (tenantId is null) or belong to target tenant
        if (targetRole.getTenantId() != null && !targetRole.getTenantId().equals(cleanTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign role belonging to another tenant.");
        }

        // 2. Privilege Escalation Prevention:
        // Admin Delegate or ordinary Tenant Admin cannot assign PLATFORM_ADMIN or protected system roles!
        boolean isActorPlatformAdmin = "PLATFORM_ADMIN".equalsIgnoreCase(actorRole) || "SUPER_ADMIN".equalsIgnoreCase(actorRole);
        if (targetRole.isProtected() && !isActorPlatformAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Privilege escalation denied: You lack authority to assign protected role '" + targetRole.getRoleCode() + "'.");
        }

        if ("ADMIN_DELEGATE".equalsIgnoreCase(actorRole)) {
            if ("TENANT_ADMIN".equalsIgnoreCase(targetRole.getRoleCode()) || "PLATFORM_ADMIN".equalsIgnoreCase(targetRole.getRoleCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin Delegate cannot assign Tenant Administrator or Platform Administrator roles.");
            }
        }

        // Check if already assigned
        UserRole existing = userRoleRepository.findByUserIdAndRoleIdAndTenantId(targetUserId, req.getRoleId(), cleanTenant)
                .orElse(null);

        if (existing != null) {
            existing.setPrimary(req.isPrimary());
            existing.setAssignedBy(actorUsername);
            UserRole saved = userRoleRepository.save(existing);
            invalidateCache(cleanTenant, targetUserId);
            return toUserRoleDto(saved);
        }

        UserRole userRole = UserRole.builder()
                .user(targetUser)
                .role(targetRole)
                .tenantId(cleanTenant)
                .isPrimary(req.isPrimary())
                .assignedBy(actorUsername)
                .build();

        UserRole saved = userRoleRepository.save(userRole);
        invalidateCache(cleanTenant, targetUserId);

        auditService.log(cleanTenant, null, actorUsername, actorRole, "ASSIGN_USER_ROLE",
                "Assigned role " + targetRole.getRoleCode() + " to user " + targetUser.getUsername());

        return toUserRoleDto(saved);
    }

    @Transactional
    public void revokeUserRole(Long targetUserId, Long roleId, String tenantId, Long actorUserId, String actorUsername, String actorRole) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";

        // Self-escalation / self-mutation protection
        if (targetUserId.equals(actorUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot alter your own roles.");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found."));

        if ("ADMIN_DELEGATE".equalsIgnoreCase(actorRole) && "TENANT_ADMIN".equalsIgnoreCase(role.getRoleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin Delegate cannot revoke primary Tenant Administrator role.");
        }

        userRoleRepository.deleteByUserIdAndRoleIdAndTenantId(targetUserId, roleId, cleanTenant);
        invalidateCache(cleanTenant, targetUserId);

        auditService.log(cleanTenant, null, actorUsername, actorRole, "REVOKE_USER_ROLE",
                "Revoked role " + role.getRoleCode() + " from user id " + targetUserId);
    }

    // =========================================================================
    // CACHING & HELPERS
    // =========================================================================

    public void invalidateCache(String tenantId, Long userId) {
        if (tenantId != null && userId != null) {
            authorizationCache.remove(tenantId + ":" + userId);
        }
    }

    public void invalidateCacheForTenant(String tenantId) {
        if (tenantId != null) {
            authorizationCache.keySet().removeIf(k -> k.startsWith(tenantId + ":"));
        }
    }

    private String chooseWiderScope(String s1, String s2) {
        int w1 = scopeWeight(s1);
        int w2 = scopeWeight(s2);
        return w1 >= w2 ? s1 : s2;
    }

    private int scopeWeight(String s) {
        if (s == null) return 0;
        return switch (s.toUpperCase()) {
            case "ALL" -> 5;
            case "TENANT" -> 4;
            case "ASSIGNED_CLASS" -> 3;
            case "ASSIGNED" -> 2;
            case "OWN" -> 1;
            default -> 0;
        };
    }

    private RoleDto toRoleDto(Role r) {
        List<RolePermission> rps = rolePermissionRepository.findByRoleId(r.getId());
        List<RolePermissionAssignmentDto> pAssignments = rps.stream().map(rp -> RolePermissionAssignmentDto.builder()
                .permissionKey(rp.getPermission().getPermissionKey())
                .defaultScope(rp.getDefaultScope())
                .build()).collect(Collectors.toList());

        return RoleDto.builder()
                .id(r.getId())
                .tenantId(r.getTenantId())
                .roleCode(r.getRoleCode())
                .displayName(r.getDisplayName())
                .description(r.getDescription())
                .isSystem(r.isSystem())
                .isProtected(r.isProtected())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .permissions(pAssignments)
                .build();
    }

    private UserRoleDto toUserRoleDto(UserRole ur) {
        return UserRoleDto.builder()
                .id(ur.getId())
                .userId(ur.getUser().getId())
                .username(ur.getUser().getUsername())
                .roleId(ur.getRole().getId())
                .roleCode(ur.getRole().getRoleCode())
                .roleDisplayName(ur.getRole().getDisplayName())
                .tenantId(ur.getTenantId())
                .isPrimary(ur.isPrimary())
                .assignedAt(ur.getAssignedAt())
                .assignedBy(ur.getAssignedBy())
                .build();
    }
}
