package com.successacademy.authservice.config;

import com.successacademy.authservice.model.entity.Permission;
import com.successacademy.authservice.model.entity.Role;
import com.successacademy.authservice.model.entity.RolePermission;
import com.successacademy.authservice.repository.PermissionRepository;
import com.successacademy.authservice.repository.RolePermissionRepository;
import com.successacademy.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@Order(12)
@RequiredArgsConstructor
@Slf4j
public class UserManagementDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRolePermissions();
    }

    private void seedPermissions() {
        createPermIfNotExists("user.view", "USER", "VIEW", "View tenant users, profiles, and status", "SECURITY", true, false);
        createPermIfNotExists("user.create", "USER", "CREATE", "Create and register new tenant users", "SECURITY", true, false);
        createPermIfNotExists("user.update", "USER", "UPDATE", "Update user profile and contact details", "SECURITY", true, false);
        createPermIfNotExists("user.activate", "USER", "ACTIVATE", "Activate user account", "SECURITY", true, false);
        createPermIfNotExists("user.deactivate", "USER", "DEACTIVATE", "Deactivate user account", "SECURITY", true, false);
        createPermIfNotExists("user.suspend", "USER", "SUSPEND", "Suspend user account", "SECURITY", true, false);
        createPermIfNotExists("user.role.assign", "USER", "ASSIGN_ROLE", "Assign roles to tenant users", "SECURITY", true, false);
        createPermIfNotExists("user.role.revoke", "USER", "REVOKE_ROLE", "Revoke roles from tenant users", "SECURITY", true, false);
        createPermIfNotExists("user.password.reset", "USER", "RESET_PASSWORD", "Reset or force-reset user passwords", "SECURITY", true, false);
        createPermIfNotExists("user.invite", "USER", "INVITE", "Send and manage user invitations", "SECURITY", true, false);
    }

    private void createPermIfNotExists(String key, String resource, String action, String desc, String cat, boolean sys, boolean prot) {
        if (permissionRepository.findByPermissionKey(key).isEmpty()) {
            Permission p = Permission.builder()
                    .permissionKey(key)
                    .resourceName(resource)
                    .actionName(action)
                    .description(desc)
                    .category(cat)
                    .isSystem(sys)
                    .isProtected(prot)
                    .build();
            permissionRepository.save(p);
            log.info("Seeded permission: {}", key);
        }
    }

    private void seedRolePermissions() {
        // TENANT_ADMIN permissions
        roleRepository.findByRoleCodeAndTenantIdIsNull("TENANT_ADMIN")
                .ifPresent(r -> assignPermsToRole(r, Set.of(
                        "user.view", "user.create", "user.update", "user.activate",
                        "user.deactivate", "user.suspend", "user.role.assign", "user.role.revoke",
                        "user.password.reset", "user.invite"
                ), "TENANT"));

        // ADMIN_DELEGATE permissions
        roleRepository.findByRoleCodeAndTenantIdIsNull("ADMIN_DELEGATE")
                .ifPresent(r -> assignPermsToRole(r, Set.of(
                        "user.view", "user.create", "user.update", "user.activate",
                        "user.deactivate", "user.role.assign", "user.password.reset", "user.invite"
                ), "TENANT"));

        // PLATFORM_ADMIN permissions
        roleRepository.findByRoleCodeAndTenantIdIsNull("PLATFORM_ADMIN")
                .ifPresent(r -> assignPermsToRole(r, Set.of(
                        "user.view", "user.create", "user.update", "user.activate",
                        "user.deactivate", "user.suspend", "user.role.assign", "user.role.revoke",
                        "user.password.reset", "user.invite"
                ), "ALL"));
    }

    private void assignPermsToRole(Role role, Set<String> permKeys, String defaultScope) {
        List<RolePermission> existing = rolePermissionRepository.findByRoleId(role.getId());
        Set<String> alreadyAssigned = existing.stream()
                .map(rp -> rp.getPermission().getPermissionKey())
                .collect(java.util.stream.Collectors.toSet());

        for (String key : permKeys) {
            if (!alreadyAssigned.contains(key)) {
                permissionRepository.findByPermissionKey(key).ifPresent(p -> {
                    RolePermission rp = RolePermission.builder()
                            .role(role)
                            .permission(p)
                            .defaultScope(defaultScope)
                            .build();
                    rolePermissionRepository.save(rp);
                });
            }
        }
    }
}
