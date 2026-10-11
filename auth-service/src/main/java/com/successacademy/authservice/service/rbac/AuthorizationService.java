package com.successacademy.authservice.service.rbac;

import com.successacademy.authservice.dto.rbac.EffectiveAuthorizationResponse;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.ScopeType;
import com.successacademy.authservice.service.config.FeatureEntitlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final RbacService rbacService;
    private final FeatureEntitlementService featureEntitlementService;

    /**
     * Centralized decision engine evaluating:
     * User + Tenant + Feature + Permission + Scope + Resource ownership.
     */
    public boolean isAllowed(Long userId, String tenantId, String permissionKey,
                             ScopeType requestedScope, Map<String, Object> resourceContext) {

        if (userId == null || tenantId == null || permissionKey == null) {
            return false;
        }

        String cleanTenant = tenantId.trim();

        // 1. Feature Entitlement check
        String requiredFeature = resolveFeatureKey(permissionKey, resourceContext);
        if (requiredFeature != null && !requiredFeature.isBlank()) {
            boolean featureEnabled = featureEntitlementService.isFeatureEnabled(
                    cleanTenant,
                    featureEntitlementService.getRuntimeEnvironment(),
                    requiredFeature
            );
            if (!featureEnabled) {
                log.debug("Authorization DENIED: Feature '{}' is disabled for tenant '{}'", requiredFeature, cleanTenant);
                return false;
            }
        }

        // 2. Fetch Effective Authorization for User & Tenant
        EffectiveAuthorizationResponse authz = rbacService.getEffectiveAuthorization(userId, cleanTenant);
        if (authz == null || authz.getPermissions() == null) {
            return false;
        }

        // Platform Admin always allowed across all tenants
        if (authz.getRoles() != null && (authz.getRoles().contains("PLATFORM_ADMIN") || authz.getRoles().contains("SUPER_ADMIN"))) {
            return true;
        }

        // 3. Permission existence check
        if (!authz.getPermissions().contains(permissionKey)) {
            log.debug("Authorization DENIED: User {} lacks permission '{}'", userId, permissionKey);
            return false;
        }

        // 4. Scope evaluation
        String grantedScopeStr = authz.getPermissionScopes() != null ? authz.getPermissionScopes().get(permissionKey) : null;
        if (grantedScopeStr == null) {
            grantedScopeStr = "TENANT"; // Default fallback
        }

        if (requestedScope != null) {
            boolean scopeValid = isScopeSatisfied(grantedScopeStr, requestedScope);
            if (!scopeValid) {
                log.debug("Authorization DENIED: Scope violation. Granted: {}, Requested: {}", grantedScopeStr, requestedScope);
                return false;
            }
        }

        // 5. Resource context validation (e.g. OWN or ASSIGNED validation)
        if (resourceContext != null) {
            if ("OWN".equalsIgnoreCase(grantedScopeStr)) {
                Object ownerIdObj = resourceContext.get("ownerUserId");
                if (ownerIdObj != null) {
                    Long ownerId = parseLong(ownerIdObj);
                    if (ownerId != null && !ownerId.equals(userId)) {
                        log.debug("Authorization DENIED: Resource owner {} does not match actor {}", ownerId, userId);
                        return false;
                    }
                }
            }

            if ("ASSIGNED".equalsIgnoreCase(grantedScopeStr) || "ASSIGNED_CLASS".equalsIgnoreCase(grantedScopeStr)) {
                Object assignedTeacherIdObj = resourceContext.get("assignedTeacherId");
                Object currentTeacherIdObj = resourceContext.get("currentTeacherId");
                if (assignedTeacherIdObj != null && currentTeacherIdObj != null) {
                    Long assigned = parseLong(assignedTeacherIdObj);
                    Long current = parseLong(currentTeacherIdObj);
                    if (assigned != null && current != null && !assigned.equals(current)) {
                        log.debug("Authorization DENIED: Assigned teacher {} does not match actor teacher {}", assigned, current);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isScopeSatisfied(String grantedScopeStr, ScopeType requested) {
        int grantedWeight = scopeWeight(grantedScopeStr);
        int requestedWeight = scopeWeight(requested.name());
        return grantedWeight >= requestedWeight;
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

    private String resolveFeatureKey(String permissionKey, Map<String, Object> resourceContext) {
        if (resourceContext != null && resourceContext.containsKey("featureKey")) {
            return String.valueOf(resourceContext.get("featureKey"));
        }

        // Automatic mapping based on catalog features defined in FeatureKey.ALL_FEATURES
        if (permissionKey.startsWith("academic.session.")) return FeatureKey.ACADEMIC_SESSIONS;
        if (permissionKey.startsWith("faculty.")) return FeatureKey.FACULTY;
        if (permissionKey.startsWith("staff.")) return FeatureKey.STAFF;
        if (permissionKey.startsWith("attendance.")) return FeatureKey.ATTENDANCE;
        if (permissionKey.startsWith("fees.")) return FeatureKey.FEES;
        if (permissionKey.startsWith("exam.")) return FeatureKey.EXAMINATIONS;
        if (permissionKey.startsWith("activity.")) return FeatureKey.ACTIVITIES;
        if (permissionKey.startsWith("communication.")) return FeatureKey.COMMUNICATION;
        if (permissionKey.startsWith("notice.")) return FeatureKey.NOTICES;
        if (permissionKey.startsWith("event.")) return FeatureKey.EVENTS;
        if (permissionKey.startsWith("gallery.")) return FeatureKey.GALLERY;

        return null; // Non-feature-gated core modules (students, classes, rbac, platform)
    }

    private Long parseLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try {
            return Long.parseLong(String.valueOf(obj).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
