package com.successacademy.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.security.TenantContext;
import com.successacademy.authservice.service.config.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ConfigurationGovernanceSecurityAndLifecycleTest {

    @Mock
    private ConfigurationVersionRepository versionRepository;

    @Mock
    private TenantConfigurationRepository configRepository;

    @Mock
    private ConfigurationAuditService auditService;

    @Mock
    private ConfigurationResolverService resolverService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private ConfigurationValidationService validationService;
    private ConfigurationDiffService diffService;
    private ConfigurationVersionService versionService;

    @BeforeEach
    void setUp() {
        validationService = new ConfigurationValidationService(objectMapper);
        diffService = new ConfigurationDiffService(versionRepository, configRepository, objectMapper);
        versionService = new ConfigurationVersionService(
                versionRepository, configRepository, auditService, resolverService, validationService, objectMapper
        );
        TenantContext.setTenantId("tenant-alpha");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Validation engine blocks publish when errors exist (e.g. invalid hex color)")
    void testValidationBlocksPublishWhenErrorsExist() {
        ConfigurationVersion draft = ConfigurationVersion.builder()
                .id(10L)
                .tenantKey("tenant-alpha")
                .versionNumber(2)
                .status("DRAFT")
                .build();
        when(versionRepository.findById(10L)).thenReturn(Optional.of(draft));

        TenantConfiguration badBranding = TenantConfiguration.builder()
                .versionId(10L)
                .tenantKey("tenant-alpha")
                .category("BRANDING")
                .configData("{\"schoolName\":\"Alpha Academy\",\"primaryColor\":\"not-a-hex\"}")
                .build();
        when(configRepository.findByVersionId(10L)).thenReturn(List.of(badBranding));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                versionService.publishVersion(10L, "admin_user")
        );

        assertEquals(422, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid hex color format"));
        verify(versionRepository, never()).save(argThat(v -> "PUBLISHED".equals(v.getStatus())));
    }

    @Test
    @DisplayName("Published configurations are strictly immutable and cannot be updated")
    void testPublishedConfigurationsAreImmutable() {
        ConfigurationVersion published = ConfigurationVersion.builder()
                .id(20L)
                .tenantKey("tenant-alpha")
                .versionNumber(1)
                .status("PUBLISHED")
                .build();
        when(versionRepository.findById(20L)).thenReturn(Optional.of(published));

        UpdateVersionConfigRequest req = UpdateVersionConfigRequest.builder()
                .changeSummary("Attempting illegal edit on published")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                versionService.updateDraftConfig(20L, req, "malicious_admin")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Published configurations are immutable"));
    }

    @Test
    @DisplayName("Rollback creates a brand NEW version and never mutates historical target version")
    void testNonDestructiveRollback() {
        ConfigurationVersion targetHistorical = ConfigurationVersion.builder()
                .id(1L)
                .tenantKey("tenant-alpha")
                .versionNumber(1)
                .status("ARCHIVED")
                .publishedAt(LocalDateTime.now().minusDays(10))
                .build();
        when(versionRepository.findById(1L)).thenReturn(Optional.of(targetHistorical));

        TenantConfiguration histConfig = TenantConfiguration.builder()
                .versionId(1L)
                .tenantKey("tenant-alpha")
                .category("BRANDING")
                .configData("{\"schoolName\":\"Alpha Historical\",\"primaryColor\":\"#112233\"}")
                .build();
        when(configRepository.findByVersionId(1L)).thenReturn(List.of(histConfig));

        ConfigurationVersion currentPublished = ConfigurationVersion.builder()
                .id(2L)
                .tenantKey("tenant-alpha")
                .versionNumber(2)
                .status("PUBLISHED")
                .build();
        when(versionRepository.findPublishedForTenantGlobal("tenant-alpha")).thenReturn(List.of(currentPublished));
        when(versionRepository.findMaxVersionNumberByTenantKey("tenant-alpha")).thenReturn(2);

        when(versionRepository.save(any(ConfigurationVersion.class))).thenAnswer(invocation -> {
            ConfigurationVersion cv = invocation.getArgument(0);
            if (cv.getId() == null) cv.setId(3L);
            return cv;
        });

        when(configRepository.save(any(TenantConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VersionInfoDto rollbackResult = versionService.rollbackVersion(1L, "rollback_operator");

        // Verify that target historical version was NOT mutated
        assertEquals("ARCHIVED", targetHistorical.getStatus());
        assertEquals(1, targetHistorical.getVersionNumber());

        // Verify that a brand new version 3 was created
        assertEquals(3, rollbackResult.getVersionNumber());
        assertEquals(1, rollbackResult.getRollbackFromVersion());
        assertEquals("PUBLISHED", rollbackResult.getStatus());

        // Verify cache invalidation
        verify(resolverService).invalidateCache("tenant-alpha");
    }

    @Test
    @DisplayName("Configuration Diff correctly computes ADDED, REMOVED, CHANGED, UNCHANGED")
    void testConfigurationDiffCalculation() {
        ConfigurationVersion v1 = ConfigurationVersion.builder()
                .id(101L).tenantKey("tenant-alpha").versionNumber(1).build();
        ConfigurationVersion v2 = ConfigurationVersion.builder()
                .id(102L).tenantKey("tenant-alpha").versionNumber(2).build();

        when(versionRepository.findById(101L)).thenReturn(Optional.of(v1));
        when(versionRepository.findById(102L)).thenReturn(Optional.of(v2));

        TenantConfiguration c1 = TenantConfiguration.builder()
                .versionId(101L).tenantKey("tenant-alpha").category("BRANDING")
                .configData("{\"schoolName\":\"Alpha Old\",\"primaryColor\":\"#112233\",\"oldLogo\":\"url1\"}")
                .build();
        TenantConfiguration c2 = TenantConfiguration.builder()
                .versionId(102L).tenantKey("tenant-alpha").category("BRANDING")
                .configData("{\"schoolName\":\"Alpha New\",\"primaryColor\":\"#112233\",\"newBanner\":\"url2\"}")
                .build();

        when(configRepository.findByVersionId(101L)).thenReturn(List.of(c1));
        when(configRepository.findByVersionId(102L)).thenReturn(List.of(c2));

        ConfigDiffResponseDto diff = diffService.computeDiff(101L, 102L);

        assertEquals(101L, diff.getBaseVersionId());
        assertEquals(102L, diff.getTargetVersionId());
        assertTrue(diff.getChangedCount() >= 1, "schoolName should be CHANGED");
        assertTrue(diff.getUnchangedCount() >= 1, "primaryColor should be UNCHANGED");
        assertTrue(diff.getRemovedCount() >= 1, "oldLogo should be REMOVED");
        assertTrue(diff.getAddedCount() >= 1, "newBanner should be ADDED");
    }

    @Test
    @DisplayName("Diff and Export mask sensitive tokens/passwords")
    void testDiffAndExportMaskSensitiveSecrets() {
        ConfigurationVersion v1 = ConfigurationVersion.builder()
                .id(201L).tenantKey("tenant-alpha").versionNumber(1).build();
        ConfigurationVersion v2 = ConfigurationVersion.builder()
                .id(202L).tenantKey("tenant-alpha").versionNumber(2).build();

        when(versionRepository.findById(201L)).thenReturn(Optional.of(v1));
        when(versionRepository.findById(202L)).thenReturn(Optional.of(v2));

        TenantConfiguration c1 = TenantConfiguration.builder()
                .versionId(201L).tenantKey("tenant-alpha").category("BUSINESS")
                .configData("{\"secretKey\":\"topsecret-111\",\"currency\":\"USD\"}")
                .build();
        TenantConfiguration c2 = TenantConfiguration.builder()
                .versionId(202L).tenantKey("tenant-alpha").category("BUSINESS")
                .configData("{\"secretKey\":\"topsecret-222\",\"currency\":\"USD\"}")
                .build();

        when(configRepository.findByVersionId(201L)).thenReturn(List.of(c1));
        when(configRepository.findByVersionId(202L)).thenReturn(List.of(c2));

        ConfigDiffResponseDto diff = diffService.computeDiff(201L, 202L);

        for (ConfigDiffItemDto item : diff.getDiffs()) {
            if (item.getPath().contains("secretKey")) {
                assertEquals("******", item.getOldValue());
                assertEquals("******", item.getNewValue());
            }
        }
    }

    @Test
    @DisplayName("Optimistic locking throws HTTP 409 Conflict when version is stale")
    void testOptimisticLockingThrowsConflictOnStaleVersion() {
        ConfigurationVersion current = ConfigurationVersion.builder()
                .id(300L)
                .tenantKey("tenant-alpha")
                .versionNumber(5)
                .optLockVersion(2L)
                .status("DRAFT")
                .build();
        when(versionRepository.findById(300L)).thenReturn(Optional.of(current));

        UpdateVersionConfigRequest staleReq = UpdateVersionConfigRequest.builder()
                .changeSummary("Stale update")
                .optLockVersion(1L) // Client thought it was on version 1, but DB is on version 2
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                versionService.updateDraftConfig(300L, staleReq, "stale_admin")
        );

        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Configuration version is stale"));
    }

    @Test
    @DisplayName("Cross-tenant version operations are strictly rejected with HTTP 403 Forbidden")
    void testCrossTenantAccessForbidden() {
        ConfigurationVersion otherTenantVersion = ConfigurationVersion.builder()
                .id(400L)
                .tenantKey("tenant-beta") // Different tenant
                .versionNumber(1)
                .status("DRAFT")
                .build();
        when(versionRepository.findById(400L)).thenReturn(Optional.of(otherTenantVersion));

        UpdateVersionConfigRequest req = UpdateVersionConfigRequest.builder()
                .changeSummary("Cross-tenant attempt")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                versionService.updateDraftConfig(400L, req, "intruder")
        );

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Access denied"));
    }

    @Test
    @DisplayName("Importing configuration creates a new DRAFT and NEVER directly publishes")
    void testImportCreatesDraftNeverDirectlyPublishes() {
        when(versionRepository.findMaxVersionNumberByTenantKey("tenant-alpha")).thenReturn(3);
        when(versionRepository.save(any(ConfigurationVersion.class))).thenAnswer(invocation -> {
            ConfigurationVersion cv = invocation.getArgument(0);
            if (cv.getId() == null) cv.setId(4L);
            return cv;
        });

        Map<String, Object> categories = Map.of(
                "BRANDING", Map.of("schoolName", "Imported Academy", "primaryColor", "#2563eb")
        );
        ImportConfigRequest req = ImportConfigRequest.builder()
                .changeSummary("Imported via governance bundle")
                .categories(categories)
                .build();

        VersionInfoDto importedDraft = versionService.importConfiguration("tenant-alpha", req, "import_admin");

        assertNotNull(importedDraft);
        assertEquals("DRAFT", importedDraft.getStatus(), "Import must ALWAYS create DRAFT, never publish directly");
        assertEquals(4, importedDraft.getVersionNumber());
        verify(versionRepository, never()).save(argThat(v -> "PUBLISHED".equals(v.getStatus())));
    }
}
