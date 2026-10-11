package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceDto {
    private Long id;
    private String tenantId;
    private Long workflowDefinitionId;
    private String workflowKey;
    private String workflowName;
    private Integer workflowVersion;
    private String resourceType;
    private String resourceId;
    private String currentState;
    private Long createdByUserId;
    private String createdByUsername;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkflowTransitionDto> availableTransitions;
}
