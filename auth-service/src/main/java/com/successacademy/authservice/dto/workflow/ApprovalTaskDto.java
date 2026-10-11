package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTaskDto {
    private Long id;
    private String tenantId;
    private Long workflowInstanceId;
    private String workflowKey;
    private String workflowName;
    private String resourceType;
    private String resourceId;
    private String currentState;
    private Long requesterUserId;
    private String requesterUsername;
    private Long transitionId;
    private Integer level;
    private String status;
    private String assignedRole;
    private Long assignedTo;
    private String assignedScope;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String completedByUsername;
    private String comment;
}
