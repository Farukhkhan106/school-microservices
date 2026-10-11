package com.successacademy.authservice.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationVersionService {

    private final ConfigurationVersionRepository versionRepository;
    private final TenantConfigurationRepository configRepository;
    private final ConfigurationAuditService auditService;
    private final ConfigurationResolverService resolverService;
    private final ConfigurationValidationService validationService;
    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_KEY_PATTERNS = Set.of(
            "password", "secret", "token", "credential", "privatekey", "apikey"
    );

    @Transactional
    public VersionInfoDto createDraftVersion(CreateVersionRequest req, String actorUsername) {
        String cleanTenant = (req.getTenantKey() != null && !req.getTenantKey().isBlank())
                ? req.getTenantKey().trim()
                : TenantContext.getTenantId();
        String cleanEnv = (req.getEnvironmentKey() != null && !req.getEnvironmentKey().isBlank())
                ? req.getEnvironmentKey().trim().toUpperCase()
                : null;

        checkTenantAccess(cleanTenant);

        Integer maxVersion = versionRepository.findMaxVersionNumberByTenantKey(cleanTenant);
        int nextVersionNumber = (maxVersion != null) ? maxVersion + 1 : 1;

        ConfigurationVersion version = ConfigurationVersion.builder()
                .tenantKey(cleanTenant)
                .environmentKey(cleanEnv)
                .versionNumber(nextVersionNumber)
                .status("DRAFT")
                .changeSummary(req.getChangeSummary() != null ? req.getChangeSummary().trim() : "Draft version " + nextVersionNumber)
                .createdBy(actorUsername != null ? actorUsername : "system")
                .updatedBy(actorUsername != null ? actorUsername : "system")
                .build();

        ConfigurationVersion saved = versionRepository.save(version);

        // If categories provided in request, save them
        if (req.getCategories() != null) {
            for (Map.Entry<String, Object> entry : req.getCategories().entrySet()) {
                saveCategoryConfig(saved.getId(), cleanTenant, cleanEnv, entry.getKey(), entry.getValue());
            }
        } else {
            // Clone previous published configurations into this draft so draft starts with current state
            List<ConfigurationVersion> previous = (cleanEnv != null)
                    ? versionRepository.findPublishedForTenantAndEnv(cleanTenant, cleanEnv)
                    : versionRepository.findPublishedForTenantGlobal(cleanTenant);
            if (!previous.isEmpty()) {
                List<TenantConfiguration> prevConfigs = configRepository.findByVersionId(previous.get(0).getId());
                for (TenantConfiguration pc : prevConfigs) {
                    TenantConfiguration copy = TenantConfiguration.builder()
                            .versionId(saved.getId())
                            .tenantKey(cleanTenant)
                            .environmentKey(cleanEnv)
                            .category(pc.getCategory())
                            .configData(pc.getConfigData())
                            .schemaVersion(pc.getSchemaVersion())
                            .build();
                    configRepository.save(copy);
                }
            }
        }

        auditService.log(cleanTenant, cleanEnv, actorUsername, "ADMIN", "CREATE_DRAFT",
                "Created draft configuration version " + nextVersionNumber, nextVersionNumber, null);

        return mapToDto(saved);
    }

    @Transactional
    public VersionInfoDto updateDraftConfig(Long versionId, UpdateVersionConfigRequest req, String actorUsername) {
        ConfigurationVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found: " + versionId));

        checkTenantAccess(version.getTenantKey());

        // Immutability check
        if (!"DRAFT".equalsIgnoreCase(version.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot edit non-draft configuration version (status: " + version.getStatus() + "). Published configurations are immutable.");
        }

        // Concurrency & Optimistic locking check
        if (req.getOptLockVersion() != null && version.getOptLockVersion() != null) {
            if (!req.getOptLockVersion().equals(version.getOptLockVersion())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Configuration version is stale. Please refresh and review the latest version.");
            }
        }

        if (req.getChangeSummary() != null && !req.getChangeSummary().isBlank()) {
            version.setChangeSummary(req.getChangeSummary().trim());
        }
        version.setUpdatedBy(actorUsername != null ? actorUsername : "system");
        version.setUpdatedAt(LocalDateTime.now());
        versionRepository.save(version);

        if (req.getCategories() != null) {
            for (Map.Entry<String, Object> entry : req.getCategories().entrySet()) {
                saveCategoryConfig(version.getId(), version.getTenantKey(), version.getEnvironmentKey(), entry.getKey(), entry.getValue());
            }
        }

        auditService.log(version.getTenantKey(), version.getEnvironmentKey(), actorUsername, "ADMIN", "UPDATE_DRAFT",
                "Updated configuration items in draft version " + version.getVersionNumber(), version.getVersionNumber(), null);

        return mapToDto(version);
    }

    @Transactional(readOnly = true)
    public ConfigValidationResultDto validateDraft(Long versionId) {
        ConfigurationVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found: " + versionId));

        checkTenantAccess(version.getTenantKey());

        List<TenantConfiguration> configs = configRepository.findByVersionId(versionId);
        return validationService.validateConfigurations(configs);
    }

    @Transactional
    public VersionInfoDto publishVersion(Long versionId, String actorUsername) {
        ConfigurationVersion targetVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found: " + versionId));

        checkTenantAccess(targetVersion.getTenantKey());

        // Immutability check
        if ("PUBLISHED".equalsIgnoreCase(targetVersion.getStatus()) || "ARCHIVED".equalsIgnoreCase(targetVersion.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Configuration version " + targetVersion.getVersionNumber() + " is already " + targetVersion.getStatus() + ". Published configurations are immutable.");
        }

        if (!"DRAFT".equalsIgnoreCase(targetVersion.getStatus()) && !"APPROVED".equalsIgnoreCase(targetVersion.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT or APPROVED versions can be published (status: " + targetVersion.getStatus() + ")");
        }

        // 1. Validate configuration BEFORE publish
        List<TenantConfiguration> configs = configRepository.findByVersionId(versionId);
        ConfigValidationResultDto validationResult = validationService.validateConfigurations(configs);
        if (!validationResult.isValid()) {
            String firstError = validationResult.getErrors().isEmpty() ? "Unknown validation error" : validationResult.getErrors().get(0).getMessage();
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Configuration validation failed: " + firstError);
        }

        // 2. Compute SHA-256 Checksum
        String checksum = computeChecksum(configs);
        targetVersion.setChecksum(checksum);

        // 3. Archive previously published versions matching tenant + environment
        List<ConfigurationVersion> currentPublished = (targetVersion.getEnvironmentKey() != null)
                ? versionRepository.findPublishedForTenantAndEnv(targetVersion.getTenantKey(), targetVersion.getEnvironmentKey())
                : versionRepository.findPublishedForTenantGlobal(targetVersion.getTenantKey());

        LocalDateTime now = LocalDateTime.now();
        for (ConfigurationVersion cv : currentPublished) {
            cv.setStatus("ARCHIVED");
            cv.setArchivedAt(now);
            versionRepository.save(cv);
        }

        // 4. Publish target version
        targetVersion.setStatus("PUBLISHED");
        targetVersion.setPublishedBy(actorUsername != null ? actorUsername : "system");
        targetVersion.setPublishedAt(now);
        targetVersion.setUpdatedBy(actorUsername != null ? actorUsername : "system");
        targetVersion.setUpdatedAt(now);
        ConfigurationVersion published = versionRepository.save(targetVersion);

        auditService.log(targetVersion.getTenantKey(), targetVersion.getEnvironmentKey(), actorUsername, "ADMIN", "PUBLISH",
                "Published configuration version " + targetVersion.getVersionNumber(), targetVersion.getVersionNumber(), "SHA-256: " + checksum);

        // 5. Invalidate resolution cache
        resolverService.invalidateCache(targetVersion.getTenantKey());

        return mapToDto(published);
    }

    @Transactional
    public VersionInfoDto rollbackVersion(Long targetVersionId, String actorUsername) {
        ConfigurationVersion targetVersion = versionRepository.findById(targetVersionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target version for rollback not found: " + targetVersionId));

        checkTenantAccess(targetVersion.getTenantKey());

        List<TenantConfiguration> targetConfigs = configRepository.findByVersionId(targetVersionId);
        if (targetConfigs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target version has no configurations to roll back to.");
        }

        // Rollback NEVER mutates the historical target version. It creates a brand NEW version.
        String cleanTenant = targetVersion.getTenantKey();
        String cleanEnv = targetVersion.getEnvironmentKey();

        Integer maxVersion = versionRepository.findMaxVersionNumberByTenantKey(cleanTenant);
        int nextVersionNumber = (maxVersion != null) ? maxVersion + 1 : 1;

        List<ConfigurationVersion> currentPublished = (cleanEnv != null)
                ? versionRepository.findPublishedForTenantAndEnv(cleanTenant, cleanEnv)
                : versionRepository.findPublishedForTenantGlobal(cleanTenant);
        Long currentPubId = currentPublished.isEmpty() ? targetVersion.getId() : currentPublished.get(0).getId();

        ConfigurationVersion rollbackVersion = ConfigurationVersion.builder()
                .tenantKey(cleanTenant)
                .environmentKey(cleanEnv)
                .versionNumber(nextVersionNumber)
                .parentVersionId(currentPubId)
                .rollbackFromVersion(targetVersion.getVersionNumber())
                .status("DRAFT")
                .changeSummary("Non-destructive rollback to version " + targetVersion.getVersionNumber())
                .createdBy(actorUsername != null ? actorUsername : "system")
                .updatedBy(actorUsername != null ? actorUsername : "system")
                .build();

        ConfigurationVersion savedRollback = versionRepository.save(rollbackVersion);

        // Clone target configs to new rollback version
        List<TenantConfiguration> clonedConfigs = new ArrayList<>();
        for (TenantConfiguration tc : targetConfigs) {
            TenantConfiguration copy = TenantConfiguration.builder()
                    .versionId(savedRollback.getId())
                    .tenantKey(cleanTenant)
                    .environmentKey(cleanEnv)
                    .category(tc.getCategory())
                    .configData(tc.getConfigData())
                    .schemaVersion(tc.getSchemaVersion())
                    .build();
            clonedConfigs.add(configRepository.save(copy));
        }

        // Validate the rollback configuration
        ConfigValidationResultDto val = validationService.validateConfigurations(clonedConfigs);
        if (!val.isValid()) {
            String errorMsg = val.getErrors().isEmpty() ? "Unknown validation error" : val.getErrors().get(0).getMessage();
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Rollback candidate failed validation: " + errorMsg);
        }

        // Compute checksum
        String checksum = computeChecksum(clonedConfigs);
        savedRollback.setChecksum(checksum);

        // Archive previous published
        LocalDateTime now = LocalDateTime.now();
        for (ConfigurationVersion cv : currentPublished) {
            cv.setStatus("ARCHIVED");
            cv.setArchivedAt(now);
            versionRepository.save(cv);
        }

        // Publish rollback version directly
        savedRollback.setStatus("PUBLISHED");
        savedRollback.setPublishedBy(actorUsername != null ? actorUsername : "system");
        savedRollback.setPublishedAt(now);
        ConfigurationVersion publishedRollback = versionRepository.save(savedRollback);

        auditService.log(cleanTenant, cleanEnv, actorUsername, "ADMIN", "ROLLBACK",
                "Rolled back to version " + targetVersion.getVersionNumber() + " as new version " + publishedRollback.getVersionNumber(),
                publishedRollback.getVersionNumber(), "Rolled back from v" + targetVersion.getVersionNumber());

        resolverService.invalidateCache(cleanTenant);

        return mapToDto(publishedRollback);
    }

    @Transactional
    public ExportConfigResponse exportConfiguration(Long versionId, String tenantKey, String actorUsername) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.getTenantId();
        checkTenantAccess(cleanTenant);

        ConfigurationVersion version;
        if (versionId != null) {
            version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found: " + versionId));
        } else {
            List<ConfigurationVersion> published = versionRepository.findPublishedForTenantGlobal(cleanTenant);
            if (published.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No published configuration found for tenant " + cleanTenant);
            }
            version = published.get(0);
        }

        checkTenantAccess(version.getTenantKey());

        Map<String, Object> rawCategories = getVersionCategories(version.getId());
        Map<String, Object> sanitized = sanitizeCategories(rawCategories);

        auditService.log(version.getTenantKey(), version.getEnvironmentKey(), actorUsername, "ADMIN", "EXPORT",
                "Exported configuration for version " + version.getVersionNumber(), version.getVersionNumber(), null);

        return ExportConfigResponse.builder()
                .schemaVersion("1.0")
                .tenantKey(version.getTenantKey())
                .environmentKey(version.getEnvironmentKey())
                .versionNumber(version.getVersionNumber())
                .exportedAt(LocalDateTime.now().toString())
                .exportedBy(actorUsername != null ? actorUsername : "system")
                .checksum(version.getChecksum())
                .categories(sanitized)
                .build();
    }

    @Transactional
    public VersionInfoDto importConfiguration(String tenantKey, ImportConfigRequest req, String actorUsername) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.getTenantId();
        checkTenantAccess(cleanTenant);

        if (req.getCategories() == null || req.getCategories().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import payload must contain categories.");
        }

        // Validate payload structure using transient TenantConfigurations
        List<TenantConfiguration> transientConfigs = new ArrayList<>();
        for (Map.Entry<String, Object> entry : req.getCategories().entrySet()) {
            String json;
            if (entry.getValue() instanceof String s) {
                json = s;
            } else {
                try {
                    json = objectMapper.writeValueAsString(entry.getValue());
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in category: " + entry.getKey());
                }
            }
            transientConfigs.add(TenantConfiguration.builder()
                    .category(entry.getKey())
                    .configData(json)
                    .build());
        }

        ConfigValidationResultDto val = validationService.validateConfigurations(transientConfigs);
        if (!val.isValid()) {
            String err = val.getErrors().isEmpty() ? "Invalid configuration data" : val.getErrors().get(0).getMessage();
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Import validation failed: " + err);
        }

        // Import ALWAYS creates a NEW DRAFT, NEVER directly publishes
        CreateVersionRequest createReq = CreateVersionRequest.builder()
                .tenantKey(cleanTenant)
                .environmentKey(req.getEnvironmentKey())
                .changeSummary(req.getChangeSummary() != null ? req.getChangeSummary().trim() : "Imported configuration bundle")
                .categories(req.getCategories())
                .build();

        VersionInfoDto draft = createDraftVersion(createReq, actorUsername);

        auditService.log(cleanTenant, req.getEnvironmentKey(), actorUsername, "ADMIN", "IMPORT",
                "Imported configuration into draft version " + draft.getVersionNumber(), draft.getVersionNumber(), null);

        return draft;
    }

    @Transactional(readOnly = true)
    public List<VersionInfoDto> listVersions(String tenantKey) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.getTenantId();
        checkTenantAccess(cleanTenant);
        return versionRepository.findByTenantKeyOrderByVersionNumberDesc(cleanTenant).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VersionInfoDto getVersionDetails(Long versionId) {
        ConfigurationVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found: " + versionId));
        checkTenantAccess(version.getTenantKey());
        return mapToDto(version);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVersionCategories(Long versionId) {
        ConfigurationVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found: " + versionId));
        checkTenantAccess(version.getTenantKey());

        List<TenantConfiguration> configs = configRepository.findByVersionId(versionId);
        Map<String, Object> map = new LinkedHashMap<>();
        for (TenantConfiguration c : configs) {
            try {
                Object parsed = objectMapper.readValue(c.getConfigData(), Object.class);
                map.put(c.getCategory(), parsed);
            } catch (Exception e) {
                map.put(c.getCategory(), c.getConfigData());
            }
        }
        return map;
    }

    private void saveCategoryConfig(Long versionId, String tenantKey, String environmentKey, String category, Object data) {
        String cleanCategory = category.trim().toUpperCase();
        String json;
        if (data instanceof String s) {
            json = s;
        } else {
            try {
                json = objectMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON data for category " + category);
            }
        }

        Optional<TenantConfiguration> existing = configRepository.findByVersionIdAndCategory(versionId, cleanCategory);
        TenantConfiguration config = existing.orElseGet(() -> TenantConfiguration.builder()
                .versionId(versionId)
                .tenantKey(tenantKey)
                .environmentKey(environmentKey)
                .category(cleanCategory)
                .build());

        config.setConfigData(json);
        config.setSchemaVersion("v1");
        configRepository.save(config);
    }

    private String computeChecksum(List<TenantConfiguration> configs) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<TenantConfiguration> sorted = new ArrayList<>(configs);
            sorted.sort(Comparator.comparing(TenantConfiguration::getCategory));
            for (TenantConfiguration c : sorted) {
                md.update(c.getCategory().getBytes(StandardCharsets.UTF_8));
                md.update(c.getConfigData().getBytes(StandardCharsets.UTF_8));
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Checksum calculation error: {}", e.getMessage());
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private void checkTenantAccess(String entityTenantKey) {
        String currentTenant = TenantContext.getTenantId();
        if (currentTenant != null && !currentTenant.isBlank() && entityTenantKey != null) {
            if (!currentTenant.equalsIgnoreCase(entityTenantKey) &&
                !currentTenant.equalsIgnoreCase("default") &&
                !currentTenant.equalsIgnoreCase("platform")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: cross-tenant configuration access forbidden.");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeCategories(Map<String, Object> categories) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : categories.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> map) {
                sanitized.put(entry.getKey(), sanitizeMap((Map<String, Object>) map));
            } else {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }
        return sanitized;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeMap(Map<String, Object> map) {
        Map<String, Object> clean = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String lower = entry.getKey().toLowerCase();
            boolean isSensitive = SENSITIVE_KEY_PATTERNS.stream().anyMatch(lower::contains);
            if (isSensitive) {
                clean.put(entry.getKey(), "******");
            } else if (entry.getValue() instanceof Map<?, ?> subMap) {
                clean.put(entry.getKey(), sanitizeMap((Map<String, Object>) subMap));
            } else {
                clean.put(entry.getKey(), entry.getValue());
            }
        }
        return clean;
    }

    private VersionInfoDto mapToDto(ConfigurationVersion cv) {
        return VersionInfoDto.builder()
                .id(cv.getId())
                .tenantKey(cv.getTenantKey())
                .environmentKey(cv.getEnvironmentKey())
                .versionNumber(cv.getVersionNumber())
                .status(cv.getStatus())
                .changeSummary(cv.getChangeSummary())
                .parentVersionId(cv.getParentVersionId())
                .rollbackFromVersion(cv.getRollbackFromVersion())
                .checksum(cv.getChecksum())
                .optLockVersion(cv.getOptLockVersion())
                .createdBy(cv.getCreatedBy())
                .createdAt(cv.getCreatedAt())
                .updatedBy(cv.getUpdatedBy())
                .updatedAt(cv.getUpdatedAt())
                .publishedBy(cv.getPublishedBy())
                .publishedAt(cv.getPublishedAt())
                .build();
    }
}
