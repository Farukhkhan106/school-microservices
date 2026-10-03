package com.successacademy.academicservice.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingSchemeResponse {
    private Long id;
    private String name;
    private String description;
    private boolean isDefault;
    private List<GradingRuleDto> rules;
}
