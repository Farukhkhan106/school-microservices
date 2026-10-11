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
    private final ObjectMapper objectMapper;

    @Transactional
    public VersionInfoDto createDraftVersion(CreateVersionRequest req, String actorUsername) {
        String cleanTenant = (req.getTenantKey() != null && !req.getTenantKey().isBlank())
                ? req.getTenantKey().trim()
                : TenantContext.getTenantId();
        String cleanEnv = (req.getEnvironmentKey() != null && !req.getEnvironmentKey().isBlank())
                ? req.getEnvironmentKey().trim().toUpperCase()
                : null;

        Integer maxVersion = versionRepository.findMaxVersionNumberByTenantKey(cleanTenant);
        int nextVersionNumber = (maxVersion != null) ? maxVersion + 1 : 1;

        ConfigurationVersion version = ConfigurationVersion.builder()
                .tenantKey(cleanTenant)
                .environmentKey(cleanEnv)
                .versionNumber(nextVersionNumber)
                .status("DRAFT")
                .changeSummary(req.getChangeSummary() != null ? req.getChangeSummary().trim() : "Draft version " + nextVersionNumber)
                .createdBy(actorUsername != null ? actorUsername : "system")
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

        auditService.log(cleanTenant, cleanEnv, actorUsername, "ADMIN", "CREATE_VERSION",
                "Created draft configuration version " + nextVersionNumber);

        return mapToDto(saved);
    }

    @Transactional
    public VersionInfoDto updateDraftConfig(Long versionId, UpdateVersionConfigRequest req, String actorUsername) {
        ConfigurationVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found"));

        if (!"DRAFT".equalsIgnoreCase(version.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit non-draft configuration version (status: " + version.getStatus() + ")");
        }

        if (req.getChangeSummary() != null && !req.getChangeSummary().isBlank()) {
            version.setChangeSummary(req.getChangeSummary().trim());
            versionRepository.save(version);
        }

        if (req.getCategories() != null) {
            for (Map.Entry<String, Object> entry : req.getCategories().entrySet()) {
                saveCategoryConfig(version.getId(), version.getTenantKey(), version.getEnvironmentKey(), entry.getKey(), entry.getValue());
            }
        }

        auditService.log(version.getTenantKey(), version.getEnvironmentKey(), actorUsername, "ADMIN", "UPDATE_CONFIG",
                "Updated configuration items in draft version " + version.getVersionNumber());

        return mapToDto(version);
    }

    @Transactional
    public VersionInfoDto publishVersion(Long versionId, String actorUsername) {
        ConfigurationVersion targetVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration version not found"));

        if (!"DRAFT".equalsIgnoreCase(targetVersion.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only DRAFT versions can be published (status: " + targetVersion.getStatus() + ")");
        }

        // 1. Archive previously published versions matching tenant + environment
        List<ConfigurationVersion> currentPublished = (targetVersion.getEnvironmentKey() != null)
                ? versionRepository.findPublishedForTenantAndEnv(targetVersion.getTenantKey(), targetVersion.getEnvironmentKey())
                : versionRepository.findPublishedForTenantGlobal(targetVersion.getTenantKey());

        LocalDateTime now = LocalDateTime.now();
        for (ConfigurationVersion cv : currentPublished) {
            cv.setStatus("ARCHIVED");
            cv.setArchivedAt(now);
            versionRepository.save(cv);
        }

        // 2. Publish target version
        targetVersion.setStatus("PUBLISHED");
        targetVersion.setPublishedBy(actorUsername != null ? actorUsername : "system");
        targetVersion.setPublishedAt(now);
        ConfigurationVersion published = versionRepository.save(targetVersion);

        auditService.log(targetVersion.getTenantKey(), targetVersion.getEnvironmentKey(), actorUsername, "ADMIN", "PUBLISH_VERSION",
                "Published configuration version " + targetVersion.getVersionNumber());

        // 3. Invalidate resolution cache
        resolverService.invalidateCache(targetVersion.getTenantKey());

        return mapToDto(published);
    }

    public List<VersionInfoDto> listVersions(String tenantKey) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.getTenantId();
        return versionRepository.findByTenantKeyOrderByVersionNumberDesc(cleanTenant).stream()
                .map(this::mapToDto)
                .toList();
    }

    public Map<String, Object> getVersionCategories(Long versionId) {
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

    private VersionInfoDto mapToDto(ConfigurationVersion cv) {
        return VersionInfoDto.builder()
                .id(cv.getId())
                .versionNumber(cv.getVersionNumber())
                .status(cv.getStatus())
                .changeSummary(cv.getChangeSummary())
                .createdBy(cv.getCreatedBy())
                .createdAt(cv.getCreatedAt())
                .publishedBy(cv.getPublishedBy())
                .publishedAt(cv.getPublishedAt())
                .build();
    }
}
