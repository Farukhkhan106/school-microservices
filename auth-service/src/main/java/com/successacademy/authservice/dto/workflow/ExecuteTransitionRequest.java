package com.successacademy.authservice.dto.workflow;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTransitionRequest {
    private String actionKey;
    private String comment;
    private String reason;
    private String idempotencyKey;
    private Long expectedVersion; // For explicit client optimistic lock check
    private Map<String, Object> resourceContext;
}
