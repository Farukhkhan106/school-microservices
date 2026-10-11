package com.successacademy.authservice.dto.workflow;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionDto {
    private Long id;
    private String fromState;
    private String toState;
    private String actionKey;
    private String name;
    private String permissionKey;
    private String requiredScope;
    private boolean approvalRequired;
    private Integer approvalLevel;
    private boolean makerCheckerRequired;
    private boolean requiresComment;
    private boolean requiresRejectionReason;
    private boolean isActive;
}
