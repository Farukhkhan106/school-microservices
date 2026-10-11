package com.successacademy.authservice.controller;

import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.entity.ConfigurationAuditLog;
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
    private final ConfigurationValidationService validationService;
    private final ConfigurationDiffService diffService;
    private final ConfigurationAuditService auditService;
    private final FeatureEntitlementService featureService;
    private final SecurityContextUtil securityContextUtil;

    /**
     * Resolves effective configuration for the current tenant and runtime environment.
     */
    @GetMapping("/effective")
    public ResponseEntity<EffectiveConfigResponse> getEffectiveConfig(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {

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
     * Get single version metadata. (ADMIN required)
     */
    @GetMapping("/versions/{versionId}")
    public ResponseEntity<VersionInfoDto> getVersionDetails(
            @PathVariable Long versionId,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        return ResponseEntity.ok(versionService.getVersionDetails(versionId));
    }

    /**
     * Get categories for a specific configuration version. (ADMIN required)
     */
    @GetMapping("/versions/{versionId}/categories")
    public ResponseEntity<Map<String, Object>> getVersionCategories(
            @PathVariable Long versionId,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        return ResponseEntity.ok(versionService.getVersionCategories(versionId));
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
     * Validate a configuration version before publishing. (ADMIN required)
     */
    @PostMapping("/versions/{versionId}/validate")
    public ResponseEntity<ConfigValidationResultDto> validateVersion(
            @PathVariable Long versionId,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        return ResponseEntity.ok(versionService.validateDraft(versionId));
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
     * Rollback to a historical configuration version non-destructively. (ADMIN required)
     */
    @PostMapping("/versions/{versionId}/rollback")
    public ResponseEntity<VersionInfoDto> rollbackVersion(
            @PathVariable Long versionId,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(versionService.rollbackVersion(versionId, actor));
    }

    /**
     * Compare two configuration versions (Base vs Target). (ADMIN required)
     */
    @GetMapping("/versions/diff")
    public ResponseEntity<ConfigDiffResponseDto> computeDiff(
            @RequestParam("baseVersionId") Long baseVersionId,
            @RequestParam("targetVersionId") Long targetVersionId,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        return ResponseEntity.ok(diffService.computeDiff(baseVersionId, targetVersionId));
    }

    /**
     * Export configuration bundle. (ADMIN required)
     */
    @GetMapping("/export")
    public ResponseEntity<ExportConfigResponse> exportConfiguration(
            @RequestParam(value = "versionId", required = false) Long versionId,
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(versionService.exportConfiguration(versionId, tenantParam, actor));
    }

    /**
     * Import configuration bundle into a new draft version. (ADMIN required)
     */
    @PostMapping("/import")
    public ResponseEntity<VersionInfoDto> importConfiguration(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            @RequestBody ImportConfigRequest req,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String actor = securityContextUtil.getCurrentUsername(request);
        return ResponseEntity.ok(versionService.importConfiguration(tenantParam, req, actor));
    }

    /**
     * Get configuration audit history. (ADMIN required)
     */
    @GetMapping("/audit")
    public ResponseEntity<List<ConfigurationAuditLog>> getAuditLogs(
            @RequestParam(value = "tenant", required = false) String tenantParam,
            HttpServletRequest request) {
        securityContextUtil.requireAdmin(request);
        String targetTenant = (tenantParam != null && !tenantParam.isBlank())
                ? tenantParam.trim()
                : TenantContext.getTenantId();
        return ResponseEntity.ok(auditService.getLogsForTenant(targetTenant));
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
