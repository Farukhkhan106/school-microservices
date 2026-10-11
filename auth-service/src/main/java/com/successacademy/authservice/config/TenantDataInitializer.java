package com.successacademy.authservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.*;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.entity.*;
import com.successacademy.authservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(10) // runs after basic users seeded
@RequiredArgsConstructor
@Slf4j
public class TenantDataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final AppEnvironmentRepository environmentRepository;
    private final ConfigurationVersionRepository versionRepository;
    private final TenantConfigurationRepository configRepository;
    private final FeatureEntitlementRepository featureRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) {
        seedEnvironments();
        seedDefaultTenant();
        backfillUserTenantId();
    }

    private void seedEnvironments() {
        seedEnv("DEV", "Development", false);
        seedEnv("QA", "Quality Assurance", false);
        seedEnv("UAT", "User Acceptance Testing", false);
        seedEnv("PROD", "Production", true);
    }

    private void seedEnv(String key, String displayName, boolean isProd) {
        if (!environmentRepository.existsByEnvironmentKey(key)) {
            AppEnvironment env = AppEnvironment.builder()
                    .environmentKey(key)
                    .displayName(displayName)
                    .isProduction(isProd)
                    .status("ACTIVE")
                    .build();
            environmentRepository.save(env);
            log.info("Seeded environment: {}", key);
        }
    }

    private void seedDefaultTenant() {
        String defaultKey = "default";
        if (tenantRepository.findByTenantKey(defaultKey).isEmpty()) {
            Tenant tenant = Tenant.builder()
                    .tenantKey(defaultKey)
                    .displayName("Success Academy")
                    .legalName("Success Academy School Satwas")
                    .status("ACTIVE")
                    .domain("successacademy.edu.in")
                    .contactEmail("info@successacademy.edu.in")
                    .contactPhone("+91 98765 43210")
                    .build();
            tenantRepository.save(tenant);
            log.info("Seeded default tenant: Success Academy");

            // Create baseline published version 1
            ConfigurationVersion v1 = ConfigurationVersion.builder()
                    .tenantKey(defaultKey)
                    .versionNumber(1)
                    .status("PUBLISHED")
                    .changeSummary("Initial baseline configuration")
                    .createdBy("system")
                    .publishedBy("system")
                    .publishedAt(LocalDateTime.now())
                    .build();
            ConfigurationVersion savedV1 = versionRepository.save(v1);

            // Seed Categories
            saveCategory(savedV1.getId(), defaultKey, "BRANDING", BrandingConfigDto.builder()
                    .schoolName("Success Academy")
                    .fullName("Success Academy School Satwas")
                    .tagline("शिक्षा • संस्कार • अनुशासन")
                    .motto("Education • Values • Discipline")
                    .logoUrl("/assets/school-logo.png")
                    .faviconUrl("/favicon.ico")
                    .primaryColor("#f59e0b")
                    .secondaryColor("#1e293b")
                    .accentColor("#10b981")
                    .build());

            saveCategory(savedV1.getId(), defaultKey, "ORGANIZATION", OrganizationConfigDto.builder()
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
                    .build());

            saveCategory(savedV1.getId(), defaultKey, "NAVIGATION", NavigationConfigDto.builder()
                    .enabledModules(FeatureKey.ALL_FEATURES)
                    .defaultLandingPage("/")
                    .build());

            saveCategory(savedV1.getId(), defaultKey, "DASHBOARD", DashboardConfigDto.builder()
                    .showKpiCards(true)
                    .showAttendanceAnalytics(true)
                    .showUpcomingEvents(true)
                    .showRecentNotices(true)
                    .build());

            saveCategory(savedV1.getId(), defaultKey, "BUSINESS", BusinessConfigDto.builder()
                    .currency("INR")
                    .timezone("Asia/Kolkata")
                    .dateFormat("dd/MM/yyyy")
                    .academicYearStartMonth("APRIL")
                    .build());

            // Seed feature entitlements for default tenant
            for (String feature : FeatureKey.ALL_FEATURES) {
                FeatureEntitlement fe = FeatureEntitlement.builder()
                        .tenantKey(defaultKey)
                        .featureKey(feature)
                        .enabled(true)
                        .description("Default entitlement for " + feature)
                        .updatedBy("system")
                        .build();
                featureRepository.save(fe);
            }
            log.info("Seeded feature entitlements for default tenant");
        }
    }

    private void backfillUserTenantId() {
        userRepository.findAll().forEach(u -> {
            if (u.getTenantId() == null || u.getTenantId().isBlank()) {
                u.setTenantId("default");
                userRepository.save(u);
            }
        });
    }

    private void saveCategory(Long versionId, String tenantKey, String category, Object data) {
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
        } catch (Exception e) {
            log.error("Failed to save category {}: {}", category, e.getMessage());
        }
    }
}
