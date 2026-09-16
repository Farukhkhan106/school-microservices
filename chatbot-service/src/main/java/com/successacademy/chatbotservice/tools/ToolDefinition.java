package com.successacademy.chatbotservice.tools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolDefinition {

    private String name;
    private String description;
    private List<String> allowedRoles; // e.g. ["STUDENT"], ["TEACHER"], ["ADMIN"], ["ALL"]
    private Map<String, Object> parameters;
}
