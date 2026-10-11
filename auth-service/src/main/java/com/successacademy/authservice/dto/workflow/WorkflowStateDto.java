package com.successacademy.authservice.dto.workflow;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStateDto {
    private Long id;
    private String stateKey;
    private String name;
    private String stateType;
    private Integer displayOrder;
    private boolean isInitial;
    private boolean isTerminal;
}
