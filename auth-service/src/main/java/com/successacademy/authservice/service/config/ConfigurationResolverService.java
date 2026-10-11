package com.successacademy.authservice.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import com.successacademy.authservice.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationResolverService {

    private final TenantRepository tenantRepository;
    private final AppEnvironmentRepository environmentRepository;
    private final ConfigurationVersionRepository versionRepository;
    private final TenantConfigurationRepository configRepository;
    private final FeatureEntitlementService featureService;
    private final ObjectMapper objectMapper;

    // In-memory cache: key = tenantKey + "::" + environmentKey
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    private record CacheEntry(EffectiveConfigResponse response, long timestamp) {}

    public void invalidateCache(String tenantKey) {
        if (tenantKey == null || tenantKey.isBlank()) {
            cache.clear();
            return;
        }
        cache.keySet().removeIf(k -> k.startsWith(tenantKey + "::") || k.equals(tenantKey));
    }

    public EffectiveConfigResponse resolveEffectiveConfig(String tenantKey, String environmentKey) {
        String cleanTenant = (tenantKey != null && !tenantKey.isBlank()) ? tenantKey.trim() : TenantContext.getTenantId();
        String cleanEnv = (environmentKey != null && !environmentKey.isBlank())
                ? environmentKey.trim().toUpperCase()
                : featureService.getRuntimeEnvironment();

        String cacheKey = cleanTenant + "::" + cleanEnv;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < CACHE_TTL_MS) {
            return cached.response();
        }

        EffectiveConfigResponse resolved = buildEffectiveConfig(cleanTenant, cleanEnv);
        cache.put(cacheKey, new CacheEntry(resolved, System.currentTimeMillis()));
        return resolved;
    }

    private EffectiveConfigResponse buildEffectiveConfig(String tenantKey, String environmentKey) {
        // 1. Tenant Info
        Tenant tenant = tenantRepository.findByTenantKey(tenantKey).orElse(null);
        TenantDto tenantDto;
        if (tenant != null) {
            tenantDto = TenantDto.builder()
                    .id(tenant.getId())
                    .tenantKey(tenant.getTenantKey())
                    .displayName(tenant.getDisplayName())
                    .legalName(tenant.getLegalName())
                    .status(tenant.getStatus())
                    .domain(tenant.getDomain())
                    .contactEmail(tenant.getContactEmail())
                    .contactPhone(tenant.getContactPhone())
                    .createdAt(tenant.getCreatedAt())
                    .updatedAt(tenant.getUpdatedAt())
                    .build();
        } else {
            tenantDto = TenantDto.builder()
                    .tenantKey(tenantKey)
                    .displayName("Success Academy")
                    .legalName("Success Academy School Satwas")
                    .status("ACTIVE")
                    .build();
        }

        // 2. Environment Info
        AppEnvironment env = environmentRepository.findByEnvironmentKey(environmentKey).orElse(null);
        EnvironmentDto envDto;
        if (env != null) {
            envDto = EnvironmentDto.builder()
                    .environmentKey(env.getEnvironmentKey())
                    .displayName(env.getDisplayName())
                    .isProduction(env.isProduction())
                    .status(env.getStatus())
                    .build();
        } else {
            envDto = EnvironmentDto.builder()
                    .environmentKey(environmentKey)
                    .displayName(environmentKey)
                    .isProduction("PROD".equalsIgnoreCase(environmentKey))
                    .status("ACTIVE")
                    .build();
        }

        // 3. Find active published version
        ConfigurationVersion activeVersion = null;
        List<ConfigurationVersion> envVersions = versionRepository.findPublishedForTenantAndEnv(tenantKey, environmentKey);
        if (!envVersions.isEmpty()) {
            activeVersion = envVersions.get(0);
        } else {
            List<ConfigurationVersion> globalVersions = versionRepository.findPublishedForTenantGlobal(tenantKey);
            if (!globalVersions.isEmpty()) {
                activeVersion = globalVersions.get(0);
            }
        }

        VersionInfoDto versionDto = null;
        Map<String, String> categoryConfigs = new HashMap<>();

        if (activeVersion != null) {
            versionDto = VersionInfoDto.builder()
                    .id(activeVersion.getId())
                    .versionNumber(activeVersion.getVersionNumber())
                    .status(activeVersion.getStatus())
                    .changeSummary(activeVersion.getChangeSummary())
                    .createdBy(activeVersion.getCreatedBy())
                    .createdAt(activeVersion.getCreatedAt())
                    .publishedBy(activeVersion.getPublishedBy())
                    .publishedAt(activeVersion.getPublishedAt())
                    .build();

            List<TenantConfiguration> configs = configRepository.findByVersionId(activeVersion.getId());
            for (TenantConfiguration c : configs) {
                categoryConfigs.put(c.getCategory().toUpperCase(), c.getConfigData());
            }
        } else {
            versionDto = VersionInfoDto.builder()
                    .versionNumber(1)
                    .status("PUBLISHED")
                    .changeSummary("Default baseline configuration")
                    .build();
        }

        // 4. Resolve Categories with safe defaults
        BrandingConfigDto branding = parseOrFallback(
                categoryConfigs.get("BRANDING"),
                BrandingConfigDto.class,
                defaultBranding(tenantDto.getDisplayName(), tenantDto.getLegalName())
        );

        OrganizationConfigDto organization = parseOrFallback(
                categoryConfigs.get("ORGANIZATION"),
                OrganizationConfigDto.class,
                defaultOrganization()
        );

        NavigationConfigDto navigation = parseOrFallback(
                categoryConfigs.get("NAVIGATION"),
                NavigationConfigDto.class,
                defaultNavigation()
        );

        DashboardConfigDto dashboard = parseOrFallback(
                categoryConfigs.get("DASHBOARD"),
                DashboardConfigDto.class,
                defaultDashboard()
        );

        BusinessConfigDto business = parseOrFallback(
                categoryConfigs.get("BUSINESS"),
                BusinessConfigDto.class,
                defaultBusiness()
        );

        // 5. Feature entitlements
        Map<String, Boolean> features = featureService.getEffectiveFeatures(tenantKey, environmentKey);

        // Filter navigation enabled modules according to feature entitlements
        if (navigation.getEnabledModules() != null) {
            List<String> activeModules = navigation.getEnabledModules().stream()
                    .filter(m -> Boolean.TRUE.equals(features.getOrDefault(m, true)))
                    .toList();
            navigation.setEnabledModules(activeModules);
        }

        return EffectiveConfigResponse.builder()
                .tenant(tenantDto)
                .environment(envDto)
                .version(versionDto)
                .branding(branding)
                .organization(organization)
                .features(features)
                .navigation(navigation)
                .dashboard(dashboard)
                .business(business)
                .build();
    }

    private <T> T parseOrFallback(String json, Class<T> clazz, T fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse category JSON config for {}: {}", clazz.getSimpleName(), e.getMessage());
            return fallback;
        }
    }

    private BrandingConfigDto defaultBranding(String displayName, String legalName) {
        return BrandingConfigDto.builder()
                .schoolName(displayName != null ? displayName : "Success Academy")
                .fullName(legalName != null ? legalName : "Success Academy School Satwas")
                .tagline("शिक्षा • संस्कार • अनुशासन")
                .motto("Education • Values • Discipline")
                .logoUrl("/assets/school-logo.png")
                .faviconUrl("/favicon.ico")
                .primaryColor("#f59e0b")
                .secondaryColor("#1e293b")
                .accentColor("#10b981")
                .build();
    }

    private OrganizationConfigDto defaultOrganization() {
        return OrganizationConfigDto.builder()
                .address("Success Academy School, Satwas, Madhya Pradesh — 455001, India")
                .city("Satwas")
                .state("Madhya Pradesh")
                .pincode("455001")
                .country("India")
                .contactEmail("info@successacademy.edu.in")
                .contactPhone("+91 98765 43210")
                .workingHours("Mon - Sat: 8:00 AM - 4:00 PM")
                .officeHours("8:00 AM – 4:00 PM (Monday to Saturday)")
                .curriculum("CBSE Pattern | English Medium")
                .affiliation("CBSE Pattern")
                .medium("English Medium")
                .mapEmbedUrl("https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d14742.664188559868!2d76.99468925!3d22.51670985!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x397d096a60340333%3A0x6b4055280b182cb0!2sSatwas%2C%20Madhya%20Pradesh!5e0!3m2!1sen!2sin!4v1700000000000!5m2!1sen!2sin")
                .build();
    }

    private NavigationConfigDto defaultNavigation() {
        return NavigationConfigDto.builder()
                .enabledModules(List.of(
                        "ATTENDANCE",
                        "FEES",
                        "EXAMINATIONS",
                        "ACTIVITIES",
                        "NOTICES",
                        "EVENTS",
                        "GALLERY",
                        "COMMUNICATION",
                        "STAFF",
                        "FACULTY",
                        "ACADEMIC_SESSIONS"
                ))
                .defaultLandingPage("/")
                .build();
    }

    private DashboardConfigDto defaultDashboard() {
        return DashboardConfigDto.builder()
                .showKpiCards(true)
                .showAttendanceAnalytics(true)
                .showUpcomingEvents(true)
                .showRecentNotices(true)
                .build();
    }

    private BusinessConfigDto defaultBusiness() {
        return BusinessConfigDto.builder()
                .currency("INR")
                .timezone("Asia/Kolkata")
                .dateFormat("dd/MM/yyyy")
                .academicYearStartMonth("APRIL")
                .build();
    }
}
