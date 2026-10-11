package com.successacademy.authservice.controller;

import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.security.SecurityContextUtil;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.config.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/configuration")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationResolverService resolverService;
    private final ConfigurationVersionService versionService;
    private final FeatureEntitlementService featureService;
    private final SecurityContextUtil securityContextUtil;

    /**
     * Resolves effective configuration for the current tenant and runtime environment.
     * Public endpoint: Accessible unauthenticated (uses default tenant or host context)
     * or authenticated (uses verified JWT tenant).
     */
    @GetMapping("/effective")
    public ResponseEntity<EffectiveConfigResponse> getEffectiveConfig(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {

        // For security: only an authenticated ADMIN can query arbitrary tenant overrides via param.
        // Normal requests resolve from server-side verified TenantContext.
        String resolvedTenant = TenantContext.getTenantId();
        if (tenantParam != null && !tenantParam.isBlank() && securityContextUtil.isAdmin(request)) {
            resolvedTenant = tenantParam.trim();
        }

        EffectiveConfigResponse config = resolverService.resolveEffectiveConfig(resolvedTenant, null);
        return ResponseEntity.ok(config);
    }

    /**
     * List all configuration versions for a tenant. (ADMIN required)
     */
    @GetMapping("/versions")
    public ResponseEntity<List<VersionInfoDto>> listVersions(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String targetTenant = (tenantParam != null && !tenantParam.isBlank())
                ? tenantParam.trim()
                : TenantContext.getTenantId();
        return ResponseEntity.ok(versionService.listVersions(targetTenant));
    }

    /**
     * Create a new draft configuration version. (ADMIN required)
     */
    @PostMapping("/versions")
    public ResponseEntity<VersionInfoDto> createDraftVersion(
            @Valid @RequestBody CreateVersionRequest req,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(versionService.createDraftVersion(req, actor));
    }

    /**
     * Update draft configuration categories. (ADMIN required)
     */
    @PutMapping("/versions/{versionId}")
    public ResponseEntity<VersionInfoDto> updateDraftConfig(
            @PathVariable Long versionId,
            @RequestBody UpdateVersionConfigRequest req,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(versionService.updateDraftConfig(versionId, req, actor));
    }

    /**
     * Publish a draft configuration version. (ADMIN required)
     */
    @PostMapping("/versions/{versionId}/publish")
    public ResponseEntity<VersionInfoDto> publishVersion(
            @PathVariable Long versionId,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(versionService.publishVersion(versionId, actor));
    }

    /**
     * Get feature entitlements for tenant. (ADMIN required)
     */
    @GetMapping("/features")
    public ResponseEntity<List<FeatureEntitlementDto>> listFeatures(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String targetTenant = (tenantParam != null && !tenantParam.isBlank())
                ? tenantParam.trim()
                : TenantContext.getTenantId();
        return ResponseEntity.ok(featureService.listEntitlementsForTenant(targetTenant));
    }

    /**
     * Update feature entitlements for tenant. (ADMIN required)
     */
    @PutMapping("/features")
    public ResponseEntity<Map<String, String>> updateFeatures(
            @RequestBody UpdateFeatureEntitlementsRequest req,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        featureService.updateEntitlements(req.getTenantKey(), req.getEnvironmentKey(), req.getFeatures(), actor);
        resolverService.invalidateCache(req.getTenantKey());
        return ResponseEntity.ok(Map.of("message", "Feature entitlements updated successfully"));
    }
}
