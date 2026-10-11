package com.successacademy.authservice.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.authservice.dto.config.ConfigDiffItemDto;
import com.successacademy.authservice.dto.config.ConfigDiffResponseDto;
import com.successacademy.authservice.model.entity.ConfigurationVersion;
import com.successacademy.authservice.model.entity.TenantConfiguration;
import com.successacademy.authservice.repository.ConfigurationVersionRepository;
import com.successacademy.authservice.repository.TenantConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationDiffService {

    private final ConfigurationVersionRepository versionRepository;
    private final TenantConfigurationRepository configRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_KEY_PATTERNS = Set.of(
            "password", "secret", "token", "credential", "privatekey", "apikey"
    );

    public ConfigDiffResponseDto computeDiff(Long baseVersionId, Long targetVersionId) {
        ConfigurationVersion baseVersion = versionRepository.findById(baseVersionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Base configuration version " + baseVersionId + " not found"));
        ConfigurationVersion targetVersion = versionRepository.findById(targetVersionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target configuration version " + targetVersionId + " not found"));

        // Ensure both versions belong to the same tenant
        if (!baseVersion.getTenantKey().equalsIgnoreCase(targetVersion.getTenantKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot compare configuration versions across different tenants");
        }

        List<TenantConfiguration> baseConfigs = configRepository.findByVersionId(baseVersionId);
        List<TenantConfiguration> targetConfigs = configRepository.findByVersionId(targetVersionId);

        Map<String, String> baseCategoryMap = new HashMap<>();
        for (TenantConfiguration c : baseConfigs) {
            baseCategoryMap.put(c.getCategory().toUpperCase(), c.getConfigData());
        }

        Map<String, String> targetCategoryMap = new HashMap<>();
        for (TenantConfiguration c : targetConfigs) {
            targetCategoryMap.put(c.getCategory().toUpperCase(), c.getConfigData());
        }

        Set<String> allCategories = new TreeSet<>(baseCategoryMap.keySet());
        allCategories.addAll(targetCategoryMap.keySet());

        List<ConfigDiffItemDto> diffItems = new ArrayList<>();
        int added = 0;
        int removed = 0;
        int changed = 0;
        int unchanged = 0;

        for (String category : allCategories) {
            String baseJson = baseCategoryMap.get(category);
            String targetJson = targetCategoryMap.get(category);

            if (baseJson == null && targetJson != null) {
                // Entire category was added
                JsonNode targetNode = parseJson(targetJson);
                List<ConfigDiffItemDto> items = flattenNode(category, category.toLowerCase(), null, targetNode);
                for (ConfigDiffItemDto item : items) {
                    item.setChangeType("ADDED");
                    diffItems.add(item);
                    added++;
                }
            } else if (baseJson != null && targetJson == null) {
                // Entire category was removed
                JsonNode baseNode = parseJson(baseJson);
                List<ConfigDiffItemDto> items = flattenNode(category, category.toLowerCase(), baseNode, null);
                for (ConfigDiffItemDto item : items) {
                    item.setChangeType("REMOVED");
                    diffItems.add(item);
                    removed++;
                }
            } else {
                // Compare nodes within category
                JsonNode baseNode = parseJson(baseJson);
                JsonNode targetNode = parseJson(targetJson);
                List<ConfigDiffItemDto> items = compareNodes(category, category.toLowerCase(), baseNode, targetNode);
                for (ConfigDiffItemDto item : items) {
                    diffItems.add(item);
                    switch (item.getChangeType()) {
                        case "ADDED" -> added++;
                        case "REMOVED" -> removed++;
                        case "CHANGED" -> changed++;
                        case "UNCHANGED" -> unchanged++;
                    }
                }
            }
        }

        return ConfigDiffResponseDto.builder()
                .baseVersionId(baseVersion.getId())
                .baseVersionNumber(baseVersion.getVersionNumber())
                .targetVersionId(targetVersion.getId())
                .targetVersionNumber(targetVersion.getVersionNumber())
                .addedCount(added)
                .removedCount(removed)
                .changedCount(changed)
                .unchangedCount(unchanged)
                .diffs(diffItems)
                .build();
    }

    private List<ConfigDiffItemDto> compareNodes(String category, String prefix, JsonNode base, JsonNode target) {
        List<ConfigDiffItemDto> list = new ArrayList<>();

        if (base.isObject() && target.isObject()) {
            Set<String> allFields = new TreeSet<>();
            base.fieldNames().forEachRemaining(allFields::add);
            target.fieldNames().forEachRemaining(allFields::add);

            for (String field : allFields) {
                String path = prefix + "." + field;
                JsonNode baseChild = base.get(field);
                JsonNode targetChild = target.get(field);

                if (baseChild == null) {
                    list.addAll(flattenNode(category, path, null, targetChild));
                    // mark all as ADDED
                    list.forEach(i -> {
                        if (i.getPath().startsWith(path) && i.getChangeType() == null) i.setChangeType("ADDED");
                    });
                } else if (targetChild == null) {
                    list.addAll(flattenNode(category, path, baseChild, null));
                    // mark all as REMOVED
                    list.forEach(i -> {
                        if (i.getPath().startsWith(path) && i.getChangeType() == null) i.setChangeType("REMOVED");
                    });
                } else if (baseChild.isObject() && targetChild.isObject()) {
                    list.addAll(compareNodes(category, path, baseChild, targetChild));
                } else {
                    Object oldVal = sanitizeValue(field, extractValue(baseChild));
                    Object newVal = sanitizeValue(field, extractValue(targetChild));
                    boolean equals = Objects.equals(baseChild, targetChild);

                    list.add(ConfigDiffItemDto.builder()
                            .category(category)
                            .path(path)
                            .oldValue(oldVal)
                            .newValue(newVal)
                            .changeType(equals ? "UNCHANGED" : "CHANGED")
                            .build());
                }
            }
        } else {
            Object oldVal = sanitizeValue(prefix, extractValue(base));
            Object newVal = sanitizeValue(prefix, extractValue(target));
            boolean equals = Objects.equals(base, target);
            list.add(ConfigDiffItemDto.builder()
                    .category(category)
                    .path(prefix)
                    .oldValue(oldVal)
                    .newValue(newVal)
                    .changeType(equals ? "UNCHANGED" : "CHANGED")
                    .build());
        }

        return list;
    }

    private List<ConfigDiffItemDto> flattenNode(String category, String path, JsonNode oldNode, JsonNode newNode) {
        List<ConfigDiffItemDto> list = new ArrayList<>();
        JsonNode activeNode = (newNode != null) ? newNode : oldNode;

        if (activeNode != null && activeNode.isObject()) {
            activeNode.fieldNames().forEachRemaining(f -> {
                String subPath = path + "." + f;
                JsonNode child = activeNode.get(f);
                if (child.isObject()) {
                    list.addAll(flattenNode(category, subPath, (oldNode != null ? child : null), (newNode != null ? child : null)));
                } else {
                    Object val = sanitizeValue(f, extractValue(child));
                    list.add(ConfigDiffItemDto.builder()
                            .category(category)
                            .path(subPath)
                            .oldValue(oldNode != null ? val : null)
                            .newValue(newNode != null ? val : null)
                            .changeType(newNode != null ? "ADDED" : "REMOVED")
                            .build());
                }
            });
        } else if (activeNode != null) {
            Object val = sanitizeValue(path, extractValue(activeNode));
            list.add(ConfigDiffItemDto.builder()
                    .category(category)
                    .path(path)
                    .oldValue(oldNode != null ? val : null)
                    .newValue(newNode != null ? val : null)
                    .changeType(newNode != null ? "ADDED" : "REMOVED")
                    .build());
        }

        return list;
    }

    private Object extractValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isTextual()) return node.asText();
        return node.toString();
    }

    private Object sanitizeValue(String keyOrPath, Object value) {
        if (value == null) return null;
        String lower = keyOrPath.toLowerCase();
        for (String sensitive : SENSITIVE_KEY_PATTERNS) {
            if (lower.contains(sensitive)) {
                return "******";
            }
        }
        return value;
    }

    private JsonNode parseJson(String json) {
        try {
            if (json == null || json.isBlank()) return objectMapper.createObjectNode();
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to parse config json: {}", json, e);
            return objectMapper.createObjectNode();
        }
    }
}
