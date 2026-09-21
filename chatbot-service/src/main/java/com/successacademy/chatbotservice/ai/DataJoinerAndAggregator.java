package com.successacademy.chatbotservice.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.chatbotservice.tools.ToolExecutionService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataJoinerAndAggregator {

    private final ToolExecutionService toolExecutionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JoinedExecutionResult {
        private String originalQuestion;
        private List<String> toolsInvoked;
        private Map<String, Object> toolOutputs; // toolName -> parsed JSON object
        private List<String> denials;            // list of denial messages
        private boolean isWriteAttempt;
        private String writeAttemptMessage;
        private String extractedClass;
        private String extractedSection;
        private String dateWindow;
        private String facilityQuery;
    }

    public JoinedExecutionResult executeAndAggregate(QueryDecompositionEngine.DecomposedQueryPlan plan,
                                                     Long userId,
                                                     String role,
                                                     Long studentId,
                                                     Long teacherId) {

        List<String> toolsInvoked = new ArrayList<>();
        Map<String, Object> outputs = new LinkedHashMap<>();
        List<String> denials = new ArrayList<>();

        if (plan.isWriteOperation()) {
            return JoinedExecutionResult.builder()
                .originalQuestion(plan.getOriginalMessage())
                .isWriteAttempt(true)
                .writeAttemptMessage("I am an information assistant and read-only for ERP data. System modifications must be performed through the authorized ERP screens.")
                .toolsInvoked(Collections.emptyList())
                .toolOutputs(Collections.emptyMap())
                .denials(Collections.emptyList())
                .build();
        }

        if (plan.getIntents() != null) {
            for (QueryDecompositionEngine.SubIntent intent : plan.getIntents()) {
                if (!intent.isAuthorized()) {
                    if (intent.getDenialReason() != null) {
                        denials.add(intent.getDenialReason());
                    }
                    continue;
                }

                String toolName = intent.getToolName();
                if (toolName == null || outputs.containsKey(toolName)) {
                    continue; // Skip duplicates or nulls
                }

                try {
                    Map<String, Object> params = intent.getParameters() != null ? intent.getParameters() : Collections.emptyMap();
                    log.info("Executing decomposed tool: [{}] with params: {}", toolName, params);
                    String rawResult = toolExecutionService.executeTool(toolName, params, userId, role, studentId, teacherId);

                    toolsInvoked.add(toolName);
                    Object parsed = parseJsonSafely(rawResult);
                    outputs.put(toolName, parsed);

                } catch (Exception e) {
                    log.error("Failed to execute tool [{}] during query aggregation: {}", toolName, e.getMessage());
                    outputs.put(toolName, Map.of("error", "Data temporarily unavailable for " + toolName));
                }
            }
        }

        return JoinedExecutionResult.builder()
            .originalQuestion(plan.getOriginalMessage())
            .toolsInvoked(toolsInvoked)
            .toolOutputs(outputs)
            .denials(denials)
            .isWriteAttempt(false)
            .extractedClass(plan.getExtractedClass())
            .extractedSection(plan.getExtractedSection())
            .dateWindow(plan.getExtractedDateWindow())
            .facilityQuery(plan.getExtractedFacility())
            .build();
    }

    private Object parseJsonSafely(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            if (json.trim().startsWith("[")) {
                return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
            } else {
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
