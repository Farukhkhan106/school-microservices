package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionDto {
    private Long id;
    private String tenantId;
    private String workflowKey;
    private String name;
    private String description;
    private String resourceType;
    private String featureKey;
    private String status;
    private Integer version;
    private boolean isSystem;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkflowStateDto> states;
    private List<WorkflowTransitionDto> transitions;
}
