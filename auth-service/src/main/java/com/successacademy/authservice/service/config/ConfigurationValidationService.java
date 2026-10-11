package com.successacademy.authservice.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.ConfigValidationErrorDto;
import com.successacademy.authservice.dto.config.ConfigValidationResultDto;
import com.successacademy.authservice.model.FeatureKey;
import com.successacademy.authservice.model.entity.TenantConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationValidationService {

    private final ObjectMapper objectMapper;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "BRANDING", "ORGANIZATION", "NAVIGATION", "DASHBOARD", "BUSINESS", "FEATURES"
    );
    private static final Set<String> PROTECTED_FIELDS = Set.of(
            "jwtsecret", "masterkey", "adminpassword", "dbpassword", "encryptionkey", "roottoken", "apisecret"
    );

    public ConfigValidationResultDto validateConfigurations(List<TenantConfiguration> configurations) {
        List<ConfigValidationErrorDto> errors = new ArrayList<>();
        List<ConfigValidationErrorDto> warnings = new ArrayList<>();

        if (configurations == null || configurations.isEmpty()) {
            warnings.add(ConfigValidationErrorDto.builder()
                    .code("EMPTY_CONFIG")
                    .path("root")
                    .message("Configuration contains no categories (empty overlay).")
                    .severity("WARNING")
                    .build());
            return new ConfigValidationResultDto(true, errors, warnings);
        }

        Set<String> seenCategories = new HashSet<>();

        for (TenantConfiguration config : configurations) {
            String category = config.getCategory() != null ? config.getCategory().trim().toUpperCase() : "";

            if (!ALLOWED_CATEGORIES.contains(category)) {
                warnings.add(ConfigValidationErrorDto.builder()
                        .code("CUSTOM_CATEGORY")
                        .path(category)
                        .message("Non-standard configuration category: " + category)
                        .severity("WARNING")
                        .build());
            }

            if (!seenCategories.add(category)) {
                errors.add(ConfigValidationErrorDto.builder()
                        .code("DUPLICATE_CATEGORY")
                        .path(category)
                        .message("Duplicate category defined in version: " + category)
                        .severity("ERROR")
                        .build());
            }

            // Validate JSON syntax
            JsonNode rootNode;
            try {
                rootNode = objectMapper.readTree(config.getConfigData());
            } catch (Exception e) {
                errors.add(ConfigValidationErrorDto.builder()
                        .code("INVALID_JSON")
                        .path(category)
                        .message("Invalid JSON syntax in category " + category + ": " + e.getMessage())
                        .severity("ERROR")
                        .build());
                continue;
            }

            // Check for protected security fields
            checkProtectedFields(category, rootNode, errors);

            // Domain-specific category validations
            switch (category) {
                case "BRANDING" -> validateBranding(rootNode, errors, warnings);
                case "ORGANIZATION" -> validateOrganization(rootNode, errors, warnings);
                case "NAVIGATION" -> validateNavigation(rootNode, errors, warnings);
                case "DASHBOARD" -> validateDashboard(rootNode, errors, warnings);
                case "BUSINESS" -> validateBusiness(rootNode, errors, warnings);
                case "FEATURES" -> validateFeatures(rootNode, errors, warnings);
            }
        }

        boolean valid = errors.isEmpty();
        return new ConfigValidationResultDto(valid, errors, warnings);
    }

    private void validateBranding(JsonNode node, List<ConfigValidationErrorDto> errors, List<ConfigValidationErrorDto> warnings) {
        if (!node.has("schoolName") || node.get("schoolName").asText().trim().isBlank()) {
            errors.add(ConfigValidationErrorDto.builder()
                    .code("REQUIRED_FIELD")
                    .path("BRANDING.schoolName")
                    .message("School name is required and cannot be empty.")
                    .severity("ERROR")
                    .build());
        }

        validateHexColor(node, "primaryColor", "BRANDING", errors);
        validateHexColor(node, "secondaryColor", "BRANDING", errors);
        validateHexColor(node, "accentColor", "BRANDING", errors);
    }

    private void validateOrganization(JsonNode node, List<ConfigValidationErrorDto> errors, List<ConfigValidationErrorDto> warnings) {
        if (node.has("contactEmail") && !node.get("contactEmail").asText().isBlank()) {
            String email = node.get("contactEmail").asText().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.add(ConfigValidationErrorDto.builder()
                        .code("INVALID_EMAIL")
                        .path("ORGANIZATION.contactEmail")
                        .message("Invalid contact email format: " + email)
                        .severity("ERROR")
                        .build());
            }
        }
    }

    private void validateNavigation(JsonNode node, List<ConfigValidationErrorDto> errors, List<ConfigValidationErrorDto> warnings) {
        if (node.has("defaultLandingPage")) {
            String page = node.get("defaultLandingPage").asText().trim();
            if (!page.startsWith("/")) {
                errors.add(ConfigValidationErrorDto.builder()
                        .code("INVALID_ROUTE")
                        .path("NAVIGATION.defaultLandingPage")
                        .message("Default landing page must start with '/' (provided: " + page + ")")
                        .severity("ERROR")
                        .build());
            }
        }

        if (node.has("enabledModules") && node.get("enabledModules").isArray()) {
            Set<String> validFeatures = new HashSet<>(FeatureKey.ALL_FEATURES);
            for (JsonNode m : node.get("enabledModules")) {
                String mod = m.asText().trim().toUpperCase();
                if (!validFeatures.contains(mod)) {
                    warnings.add(ConfigValidationErrorDto.builder()
                            .code("UNKNOWN_MODULE")
                            .path("NAVIGATION.enabledModules")
                            .message("Unknown module declared in navigation: " + mod)
                            .severity("WARNING")
                            .build());
                }
            }
        }
    }

    private void validateDashboard(JsonNode node, List<ConfigValidationErrorDto> errors, List<ConfigValidationErrorDto> warnings) {
        // Check boolean properties if present
        for (String field : List.of("showKpiCards", "showAttendanceAnalytics", "showUpcomingEvents", "showRecentNotices")) {
            if (node.has(field) && !node.get(field).isBoolean()) {
                errors.add(ConfigValidationErrorDto.builder()
                        .code("INVALID_TYPE")
                        .path("DASHBOARD." + field)
                        .message("Field " + field + " must be a boolean.")
                        .severity("ERROR")
                        .build());
            }
        }
    }

    private void validateBusiness(JsonNode node, List<ConfigValidationErrorDto> errors, List<ConfigValidationErrorDto> warnings) {
        if (node.has("currency")) {
            String curr = node.get("currency").asText().trim();
            if (curr.length() != 3) {
                warnings.add(ConfigValidationErrorDto.builder()
                        .code("NON_STANDARD_CURRENCY")
                        .path("BUSINESS.currency")
                        .message("Currency code should be a 3-letter ISO code (e.g. INR, USD).")
                        .severity("WARNING")
                        .build());
            }
        }
    }

    private void validateFeatures(JsonNode node, List<ConfigValidationErrorDto> errors, List<ConfigValidationErrorDto> warnings) {
        Set<String> validFeatures = new HashSet<>(FeatureKey.ALL_FEATURES);
        node.fieldNames().forEachRemaining(key -> {
            if (!validFeatures.contains(key.trim().toUpperCase())) {
                warnings.add(ConfigValidationErrorDto.builder()
                        .code("UNKNOWN_FEATURE_KEY")
                        .path("FEATURES." + key)
                        .message("Feature key " + key + " is not recognized in system FeatureKey catalog.")
                        .severity("WARNING")
                        .build());
            }
        });
    }

    private void validateHexColor(JsonNode node, String fieldName, String category, List<ConfigValidationErrorDto> errors) {
        if (node.has(fieldName) && !node.get(fieldName).asText().isBlank()) {
            String color = node.get(fieldName).asText().trim();
            if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
                errors.add(ConfigValidationErrorDto.builder()
                        .code("INVALID_HEX_COLOR")
                        .path(category + "." + fieldName)
                        .message("Invalid hex color format for " + fieldName + ": '" + color + "'. Expected format #RRGGBB or #RGB")
                        .severity("ERROR")
                        .build());
            }
        }
    }

    private void checkProtectedFields(String category, JsonNode node, List<ConfigValidationErrorDto> errors) {
        if (node == null || !node.isObject()) return;
        node.fieldNames().forEachRemaining(field -> {
            String lower = field.toLowerCase().replaceAll("[^a-z]", "");
            for (String protectedKey : PROTECTED_FIELDS) {
                if (lower.contains(protectedKey)) {
                    errors.add(ConfigValidationErrorDto.builder()
                            .code("PROTECTED_CONFIGURATION_FIELD")
                            .path(category + "." + field)
                            .message("Modification of platform security field '" + field + "' is forbidden.")
                            .severity("ERROR")
                            .build());
                }
            }
            JsonNode child = node.get(field);
            if (child != null && child.isObject()) {
                checkProtectedFields(category, child, errors);
            }
        });
    }
}
