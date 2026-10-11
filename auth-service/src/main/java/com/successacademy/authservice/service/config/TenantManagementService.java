package com.successacademy.authservice.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final ConfigurationVersionRepository versionRepository;
    private final TenantConfigurationRepository configRepository;
    private final ConfigurationAuditService auditService;
    private final ConfigurationResolverService resolverService;
    private final ObjectMapper objectMapper;

    @Transactional
    public TenantDto createTenant(CreateTenantRequest req, String actorUsername) {
        String cleanKey = req.getTenantKey().trim().toLowerCase();
        if (tenantRepository.existsByTenantKey(cleanKey)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant key '" + cleanKey + "' already exists");
        }

        Tenant tenant = Tenant.builder()
                .tenantKey(cleanKey)
                .displayName(req.getDisplayName().trim())
                .legalName(req.getLegalName() != null ? req.getLegalName().trim() : req.getDisplayName().trim())
                .status(req.getStatus() != null ? req.getStatus().trim().toUpperCase() : "ACTIVE")
                .domain(req.getDomain())
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .build();

        Tenant saved = tenantRepository.save(tenant);

        // Create baseline Version 1 (PUBLISHED) with initial categories
        ConfigurationVersion v1 = ConfigurationVersion.builder()
                .tenantKey(cleanKey)
                .versionNumber(1)
                .status("PUBLISHED")
                .changeSummary("Initial baseline configuration")
                .createdBy(actorUsername != null ? actorUsername : "system")
                .publishedBy(actorUsername != null ? actorUsername : "system")
                .publishedAt(LocalDateTime.now())
                .build();
        ConfigurationVersion savedV1 = versionRepository.save(v1);

        // Seed initial categories for this new tenant
        seedCategory(savedV1.getId(), cleanKey, "BRANDING", BrandingConfigDto.builder()
                .schoolName(saved.getDisplayName())
                .fullName(saved.getLegalName())
                .primaryColor("#f59e0b")
                .secondaryColor("#1e293b")
                .accentColor("#10b981")
                .build());

        seedCategory(savedV1.getId(), cleanKey, "ORGANIZATION", OrganizationConfigDto.builder()
                .contactEmail(saved.getContactEmail())
                .contactPhone(saved.getContactPhone())
                .build());

        auditService.log(cleanKey, null, actorUsername, "ADMIN", "CREATE_TENANT",
                "Created new tenant " + cleanKey + " (" + saved.getDisplayName() + ")");

        resolverService.invalidateCache(cleanKey);

        return mapToDto(saved);
    }

    @Transactional
    public TenantDto updateTenant(String tenantKey, UpdateTenantRequest req, String actorUsername) {
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant '" + tenantKey + "' not found"));

        if (req.getDisplayName() != null && !req.getDisplayName().isBlank()) {
            tenant.setDisplayName(req.getDisplayName().trim());
        }
        if (req.getLegalName() != null) {
            tenant.setLegalName(req.getLegalName().trim());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            tenant.setStatus(req.getStatus().trim().toUpperCase());
        }
        if (req.getDomain() != null) {
            tenant.setDomain(req.getDomain().trim());
        }
        if (req.getContactEmail() != null) {
            tenant.setContactEmail(req.getContactEmail().trim());
        }
        if (req.getContactPhone() != null) {
            tenant.setContactPhone(req.getContactPhone().trim());
        }

        Tenant updated = tenantRepository.save(tenant);
        auditService.log(tenantKey, null, actorUsername, "ADMIN", "UPDATE_TENANT",
                "Updated tenant profile details for " + tenantKey);

        resolverService.invalidateCache(tenantKey);
        return mapToDto(updated);
    }

    public TenantDto getTenant(String tenantKey) {
        return tenantRepository.findByTenantKey(tenantKey)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant '" + tenantKey + "' not found"));
    }

    public List<TenantDto> listTenants() {
        return tenantRepository.findAll().stream().map(this::mapToDto).toList();
    }

    private void seedCategory(Long versionId, String tenantKey, String category, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            TenantConfiguration config = TenantConfiguration.builder()
                    .versionId(versionId)
                    .tenantKey(tenantKey)
                    .category(category)
                    .configData(json)
                    .schemaVersion("v1")
                    .build();
            configRepository.save(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to seed initial category {}: {}", category, e.getMessage());
        }
    }

    private TenantDto mapToDto(Tenant t) {
        return TenantDto.builder()
                .id(t.getId())
                .tenantKey(t.getTenantKey())
                .displayName(t.getDisplayName())
                .legalName(t.getLegalName())
                .status(t.getStatus())
                .domain(t.getDomain())
                .contactEmail(t.getContactEmail())
                .contactPhone(t.getContactPhone())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
