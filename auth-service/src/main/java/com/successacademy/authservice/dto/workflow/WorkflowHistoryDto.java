package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryDto {
    private Long id;
    private Long workflowInstanceId;
    private String actionKey;
    private Long actorUserId;
    private String actorUsername;
    private String fromState;
    private String toState;
    private String comment;
    private String reason;
    private String idempotencyKey;
    private LocalDateTime createdAt;
}
