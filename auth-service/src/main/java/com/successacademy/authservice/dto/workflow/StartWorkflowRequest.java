package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartWorkflowRequest {
    private String workflowKey;
    private String resourceType;
    private String resourceId;
    private String initialComment;
    private String idempotencyKey;
    private Map<String, Object> resourceContext;
}
