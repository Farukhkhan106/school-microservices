package com.successacademy.chatbotservice.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.chatbotservice.dto.ChatMessageDto;
import com.successacademy.chatbotservice.tools.ToolDefinition;
import com.successacademy.chatbotservice.tools.ToolExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIProvider implements AIProvider {

    private final ToolExecutionService toolExecutionService;
    private final DeterministicToolFallbackProvider fallbackProvider;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.provider:openai}")
    private String configuredProvider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:gpt-4o-mini}")
    private String model;

    @Value("${ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Override
    public AIResponse chat(String userMessage,
                           String systemPrompt,
                           List<ChatMessageDto> history,
                           Long userId,
                           String role,
                           Long studentId,
                           Long teacherId,
                           Long conversationId) {

        // If no API key configured or fallback specified, use cognitive ERP tool engine
        if (apiKey == null || apiKey.trim().isBlank() || "mock".equalsIgnoreCase(configuredProvider) || "fallback".equalsIgnoreCase(configuredProvider)) {
            log.info("AI API key not configured or fallback requested. Executing cognitive ERP tool engine.");
            return fallbackProvider.handleQuery(userMessage, history, userId, role, studentId, teacherId, conversationId);
        }

        try {
            // Prepare messages
            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            // Add recent history (up to last 6 messages)
            if (history != null) {
                int start = Math.max(0, history.size() - 6);
                for (int i = start; i < history.size(); i++) {
                    ChatMessageDto m = history.get(i);
                    String msgRole = "USER".equalsIgnoreCase(m.getSender()) ? "user" : "assistant";
                    messages.add(Map.of("role", msgRole, "content", m.getMessage()));
                }
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            // Prepare tools for this role
            List<ToolDefinition> tools = toolExecutionService.getAvailableToolsForRole(role);
            List<Map<String, Object>> toolsPayload = new ArrayList<>();
            for (ToolDefinition t : tools) {
                Map<String, Object> toolMap = new HashMap<>();
                toolMap.put("type", "function");
                Map<String, Object> func = new HashMap<>();
                func.put("name", t.getName());
                func.put("description", t.getDescription());
                func.put("parameters", Map.of("type", "object", "properties", Collections.emptyMap()));
                toolMap.put("function", func);
                toolsPayload.add(toolMap);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            if (!toolsPayload.isEmpty()) {
                requestBody.put("tools", toolsPayload);
                requestBody.put("tool_choice", "auto");
            }
            requestBody.put("temperature", 0.3);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "/chat/completions";

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("AI API returned status {}, falling back to cognitive ERP tools.", response.getStatusCode());
                return fallbackProvider.handleQuery(userMessage, history, userId, role, studentId, teacherId, conversationId);
            }

            Map<String, Object> respMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                return fallbackProvider.handleQuery(userMessage, history, userId, role, studentId, teacherId, conversationId);
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> messageResp = (Map<String, Object>) firstChoice.get("message");
            if (messageResp == null) {
                return fallbackProvider.handleQuery(userMessage, history, userId, role, studentId, teacherId, conversationId);
            }

            // Check if model wants to call tools (supports multiple tools in one turn)
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) messageResp.get("tool_calls");
            if (toolCalls != null && !toolCalls.isEmpty()) {
                messages.add(messageResp);
                List<String> invokedNames = new ArrayList<>();

                for (Map<String, Object> toolCall : toolCalls) {
                    Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                    String toolName = (String) function.get("name");
                    invokedNames.add(toolName);
                    log.info("Model requested tool call: {}", toolName);

                    Map<String, Object> callArgs = Collections.emptyMap();
                    try {
                        String rawArgs = (String) function.get("arguments");
                        if (rawArgs != null && !rawArgs.isBlank()) {
                            callArgs = objectMapper.readValue(rawArgs, new TypeReference<>() {});
                        }
                    } catch (Exception ignored) {}

                    String toolOutput = toolExecutionService.executeTool(toolName, callArgs, userId, role, studentId, teacherId);

                    Map<String, Object> toolMsg = new HashMap<>();
                    toolMsg.put("role", "tool");
                    toolMsg.put("tool_call_id", toolCall.get("id"));
                    toolMsg.put("content", toolOutput);
                    messages.add(toolMsg);
                }

                requestBody.put("messages", messages);
                requestBody.remove("tools");
                requestBody.remove("tool_choice");

                HttpEntity<Map<String, Object>> secondEntity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> secondResp = restTemplate.postForEntity(url, secondEntity, String.class);
                if (secondResp.getStatusCode().is2xxSuccessful() && secondResp.getBody() != null) {
                    Map<String, Object> secondMap = objectMapper.readValue(secondResp.getBody(), new TypeReference<>() {});
                    List<Map<String, Object>> secondChoices = (List<Map<String, Object>>) secondMap.get("choices");
                    if (secondChoices != null && !secondChoices.isEmpty()) {
                        Map<String, Object> finalMsg = (Map<String, Object>) secondChoices.get(0).get("message");
                        String finalContent = (String) finalMsg.get("content");
                        return AIResponse.builder()
                            .content(finalContent)
                            .toolUsed(String.join(", ", invokedNames))
                            .toolsInvoked(invokedNames)
                            .fallbackUsed(false)
                            .build();
                    }
                }

                // If second LLM call failed, fallback format
                return fallbackProvider.handleQuery(userMessage, history, userId, role, studentId, teacherId, conversationId);
            }

            // Direct text response from LLM
            String content = (String) messageResp.get("content");
            return AIResponse.builder()
                .content(content != null ? content : "No response generated.")
                .fallbackUsed(false)
                .build();

        } catch (Exception e) {
            log.error("OpenAI provider call failed: {}, falling back to cognitive ERP tools.", e.getMessage());
            return fallbackProvider.handleQuery(userMessage, history, userId, role, studentId, teacherId, conversationId);
        }
    }
}
