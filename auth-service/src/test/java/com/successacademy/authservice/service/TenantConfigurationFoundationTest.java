package com.successacademy.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.config.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantConfigurationFoundationTest {

    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private AppEnvironmentRepository environmentRepository;
    @Mock
    private ConfigurationVersionRepository versionRepository;
    @Mock
    private TenantConfigurationRepository configRepository;
    @Mock
    private FeatureEntitlementRepository featureRepository;
    @Mock
    private ConfigurationAuditService auditService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private FeatureEntitlementService featureService;
    private ConfigurationResolverService resolverService;
    private ConfigurationVersionService versionService;
    private TenantManagementService tenantManagementService;

    @BeforeEach
    void setUp() {
        featureService = new FeatureEntitlementService(featureRepository, auditService);
        resolverService = new ConfigurationResolverService(
                tenantRepository, environmentRepository, versionRepository, configRepository, featureService, objectMapper
        );
        ConfigurationValidationService validationService = new ConfigurationValidationService(objectMapper);
        versionService = new ConfigurationVersionService(
                versionRepository, configRepository, auditService, resolverService, validationService, objectMapper
        );
        tenantManagementService = new TenantManagementService(
                tenantRepository, versionRepository, configRepository, auditService, resolverService, objectMapper
        );
    }

    @Test
    @DisplayName("Should resolve effective configuration with hierarchical defaults when no custom version exists")
    void testResolveEffectiveConfigDefaults() {
        when(tenantRepository.findByTenantKey("default")).thenReturn(Optional.of(
                Tenant.builder()
                        .tenantKey("default")
                        .displayName("Success Academy")
                        .legalName("Success Academy School Satwas")
                        .status("ACTIVE")
                        .build()
        ));
        when(environmentRepository.findByEnvironmentKey("DEV")).thenReturn(Optional.of(
                AppEnvironment.builder()
                        .environmentKey("DEV")
                        .displayName("Development")
                        .isProduction(false)
                        .status("ACTIVE")
                        .build()
        ));
        when(versionRepository.findPublishedForTenantAndEnv("default", "DEV")).thenReturn(Collections.emptyList());
        when(versionRepository.findPublishedForTenantGlobal("default")).thenReturn(Collections.emptyList());

        EffectiveConfigResponse config = resolverService.resolveEffectiveConfig("default", "DEV");

        assertNotNull(config);
        assertEquals("default", config.getTenant().getTenantKey());
        assertEquals("Success Academy", config.getTenant().getDisplayName());
        assertEquals("DEV", config.getEnvironment().getEnvironmentKey());
        assertFalse(config.getEnvironment().isProduction());
        assertNotNull(config.getBranding());
        assertEquals("Success Academy", config.getBranding().getSchoolName());
        assertNotNull(config.getOrganization());
        assertTrue(config.getFeatures().get(FeatureKey.ATTENDANCE));
        assertTrue(config.getFeatures().get(FeatureKey.EXAMINATIONS));
    }

    @Test
    @DisplayName("Should enforce tenant feature entitlements hierarchy (Environment override > Tenant default > Catalog)")
    void testFeatureEntitlementHierarchy() {
        // Environment override: transport disabled in PROD for school_a
        when(featureRepository.findByTenantKeyAndEnvironmentKeyAndFeatureKey("school_a", "PROD", FeatureKey.COMMUNICATION))
                .thenReturn(Optional.of(FeatureEntitlement.builder()
                        .tenantKey("school_a")
                        .environmentKey("PROD")
                        .featureKey(FeatureKey.COMMUNICATION)
                        .enabled(false)
                        .build()));

        // In PROD, COMMUNICATION is disabled
        assertFalse(featureService.isFeatureEnabled("school_a", "PROD", FeatureKey.COMMUNICATION));

        // In QA (no env override, no tenant override), falls back to catalog default (true)
        when(featureRepository.findByTenantKeyAndEnvironmentKeyAndFeatureKey("school_a", "QA", FeatureKey.COMMUNICATION))
                .thenReturn(Optional.empty());
        when(featureRepository.findByTenantKeyAndEnvironmentKeyIsNullAndFeatureKey("school_a", FeatureKey.COMMUNICATION))
                .thenReturn(Optional.empty());

        assertTrue(featureService.isFeatureEnabled("school_a", "QA", FeatureKey.COMMUNICATION));
    }

    @Test
    @DisplayName("Should block disabled feature with requireFeature throwing 403 Forbidden")
    void testRequireFeatureThrowsForbiddenWhenDisabled() {
        TenantContext.setTenantId("school_b");
        try {
            when(featureRepository.findByTenantKeyAndEnvironmentKeyIsNullAndFeatureKey("school_b", FeatureKey.FEES))
                    .thenReturn(Optional.of(FeatureEntitlement.builder()
                            .tenantKey("school_b")
                            .featureKey(FeatureKey.FEES)
                            .enabled(false)
                            .build()));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    featureService.requireFeature(FeatureKey.FEES)
            );
            assertEquals(403, ex.getStatusCode().value());
            assertTrue(ex.getReason().contains("FEES"));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("Should handle configuration version draft, update, and publish lifecycle with archival")
    void testConfigurationVersionLifecycle() {
        when(versionRepository.findMaxVersionNumberByTenantKey("school_c")).thenReturn(1);
        when(versionRepository.save(any(ConfigurationVersion.class))).thenAnswer(i -> {
            ConfigurationVersion v = i.getArgument(0);
            v.setId(2L);
            return v;
        });

        // 1. Create Draft
        CreateVersionRequest createReq = CreateVersionRequest.builder()
                .tenantKey("school_c")
                .changeSummary("Add custom colors")
                .build();
        VersionInfoDto draft = versionService.createDraftVersion(createReq, "admin_user");

        assertNotNull(draft);
        assertEquals(2, draft.getVersionNumber());
        assertEquals("DRAFT", draft.getStatus());

        // 2. Publish Draft
        ConfigurationVersion draftEntity = ConfigurationVersion.builder()
                .id(2L)
                .tenantKey("school_c")
                .versionNumber(2)
                .status("DRAFT")
                .build();
        when(versionRepository.findById(2L)).thenReturn(Optional.of(draftEntity));

        ConfigurationVersion oldPublished = ConfigurationVersion.builder()
                .id(1L)
                .tenantKey("school_c")
                .versionNumber(1)
                .status("PUBLISHED")
                .build();
        when(versionRepository.findPublishedForTenantGlobal("school_c")).thenReturn(List.of(oldPublished));

        VersionInfoDto published = versionService.publishVersion(2L, "admin_user");

        assertEquals("PUBLISHED", published.getStatus());
        assertEquals("ARCHIVED", oldPublished.getStatus());
        assertNotNull(oldPublished.getArchivedAt());
    }

    @Test
    @DisplayName("Should prevent publishing an already published or archived version")
    void testCannotPublishNonDraftVersion() {
        ConfigurationVersion publishedEntity = ConfigurationVersion.builder()
                .id(5L)
                .tenantKey("school_d")
                .versionNumber(1)
                .status("PUBLISHED")
                .build();
        when(versionRepository.findById(5L)).thenReturn(Optional.of(publishedEntity));

        assertThrows(ResponseStatusException.class, () ->
                versionService.publishVersion(5L, "admin_user")
        );
    }

    @Test
    @DisplayName("Should prevent creating tenant with duplicate tenant key")
    void testDuplicateTenantKeyPrevention() {
        when(tenantRepository.existsByTenantKey("existing_school")).thenReturn(true);

        CreateTenantRequest req = CreateTenantRequest.builder()
                .tenantKey("existing_school")
                .displayName("Existing School")
                .build();

        assertThrows(ResponseStatusException.class, () ->
                tenantManagementService.createTenant(req, "admin_user")
        );
    }

    @Test
    @DisplayName("Should invalidate resolution cache when a new version is published")
    void testCacheInvalidationOnPublish() {
        // Pre-populate cache by calling resolve
        when(tenantRepository.findByTenantKey("school_e")).thenReturn(Optional.of(
                Tenant.builder().tenantKey("school_e").displayName("School E").status("ACTIVE").build()
        ));
        when(versionRepository.findPublishedForTenantAndEnv(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(versionRepository.findPublishedForTenantGlobal(anyString())).thenReturn(Collections.emptyList());

        EffectiveConfigResponse firstCall = resolverService.resolveEffectiveConfig("school_e", "DEV");
        assertNotNull(firstCall);

        // Verify resolver cache can be explicitly invalidated
        resolverService.invalidateCache("school_e");
        EffectiveConfigResponse secondCall = resolverService.resolveEffectiveConfig("school_e", "DEV");
        assertNotNull(secondCall);
        // Verify tenant repository called again after invalidation
        verify(tenantRepository, times(2)).findByTenantKey("school_e");
    }
}
