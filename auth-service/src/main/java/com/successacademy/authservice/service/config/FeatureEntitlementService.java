package com.successacademy.authservice.service.config;

import com.successacademy.authservice.dto.config.FeatureEntitlementDto;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.entity.FeatureEntitlement;
import com.successacademy.authservice.repository.FeatureEntitlementRepository;
import com.successacademy.authservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureEntitlementService {

    private final FeatureEntitlementRepository featureRepository;
    private final ConfigurationAuditService auditService;

    @Value("${app.environment:${APP_ENVIRONMENT:DEV}}")
    private String runtimeEnvironment;

    public String getRuntimeEnvironment() {
        return (runtimeEnvironment != null && !runtimeEnvironment.isBlank())
                ? runtimeEnvironment.trim().toUpperCase()
                : "DEV";
    }

    /**
     * Checks if a feature is enabled for the current thread's tenant and runtime environment.
     */
    public boolean isFeatureEnabled(String featureKey) {
        String tenantKey = TenantContext.getTenantId();
        return isFeatureEnabled(tenantKey, getRuntimeEnvironment(), featureKey);
    }

    /**
     * Checks if a feature is enabled following hierarchical fallback:
     * 1. Tenant + Environment override
     * 2. Tenant default (environmentKey is null)
     * 3. Global feature catalog default (true by default)
     */
    public boolean isFeatureEnabled(String tenantKey, String environmentKey, String featureKey) {
        if (featureKey == null || featureKey.isBlank()) return false;
        String cleanFeature = featureKey.trim().toUpperCase();
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.DEFAULT_TENANT;

        // 1. Check Tenant + Environment specific override
        if (environmentKey != null && !environmentKey.isBlank()) {
            Optional<FeatureEntitlement> envOverride = featureRepository
                    .findByTenantKeyAndEnvironmentKeyAndFeatureKey(cleanTenant, environmentKey.trim().toUpperCase(), cleanFeature);
            if (envOverride.isPresent()) {
                return envOverride.get().isEnabled();
            }
        }

        // 2. Check Tenant-level default (environmentKey = null)
        Optional<FeatureEntitlement> tenantDefault = featureRepository
                .findByTenantKeyAndEnvironmentKeyIsNullAndFeatureKey(cleanTenant, cleanFeature);
        if (tenantDefault.isPresent()) {
            return tenantDefault.get().isEnabled();
        }

        // 3. Fallback to default entitlement for catalog features
        return FeatureKey.ALL_FEATURES.contains(cleanFeature);
    }

    /**
     * Backend enforcement extension point. Throws 403 Forbidden if feature is disabled.
     */
    public void requireFeature(String featureKey) {
        if (!isFeatureEnabled(featureKey)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Feature '" + featureKey + "' is not enabled for tenant '" + TenantContext.getTenantId() + "'."
            );
        }
    }

    /**
     * Resolves complete feature entitlement map for tenant and environment.
     */
    public Map<String, Boolean> getEffectiveFeatures(String tenantKey, String environmentKey) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String feature : FeatureKey.ALL_FEATURES) {
            result.put(feature, isFeatureEnabled(tenantKey, environmentKey, feature));
        }
        return result;
    }

    public List<FeatureEntitlementDto> listEntitlementsForTenant(String tenantKey) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.DEFAULT_TENANT;
        List<FeatureEntitlement> list = featureRepository.findByTenantKey(cleanTenant);
        List<FeatureEntitlementDto> dtoList = new ArrayList<>();
        for (FeatureEntitlement fe : list) {
            dtoList.add(FeatureEntitlementDto.builder()
                    .tenantKey(fe.getTenantKey())
                    .environmentKey(fe.getEnvironmentKey())
                    .featureKey(fe.getFeatureKey())
                    .enabled(fe.isEnabled())
                    .description(fe.getDescription())
                    .build());
        }
        return dtoList;
    }

    @Transactional
    public void updateEntitlements(String tenantKey, String environmentKey, Map<String, Boolean> features, String updatedBy) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.DEFAULT_TENANT;
        String cleanEnv = (environmentKey != null && !environmentKey.isBlank()) ? environmentKey.trim().toUpperCase() : null;

        for (Map.Entry<String, Boolean> entry : features.entrySet()) {
            String featureKey = entry.getKey().trim().toUpperCase();
            boolean enabled = Boolean.TRUE.equals(entry.getValue());

            Optional<FeatureEntitlement> existing = (cleanEnv != null)
                    ? featureRepository.findByTenantKeyAndEnvironmentKeyAndFeatureKey(cleanTenant, cleanEnv, featureKey)
                    : featureRepository.findByTenantKeyAndEnvironmentKeyIsNullAndFeatureKey(cleanTenant, featureKey);

            FeatureEntitlement fe = existing.orElseGet(() -> FeatureEntitlement.builder()
                    .tenantKey(cleanTenant)
                    .environmentKey(cleanEnv)
                    .featureKey(featureKey)
                    .build());

            fe.setEnabled(enabled);
            fe.setUpdatedBy(updatedBy != null ? updatedBy : "system");
            featureRepository.save(fe);
        }

        auditService.log(cleanTenant, cleanEnv, updatedBy, "ADMIN", "UPDATE_FEATURE_ENTITLEMENTS",
                "Updated " + features.size() + " feature entitlements");
    }
}
