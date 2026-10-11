package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowDefinitionRequest {
    private String workflowKey;
    private String name;
    private String description;
    private String resourceType;
    private String featureKey;
    private List<WorkflowStateDto> states;
    private List<WorkflowTransitionDto> transitions;
}
