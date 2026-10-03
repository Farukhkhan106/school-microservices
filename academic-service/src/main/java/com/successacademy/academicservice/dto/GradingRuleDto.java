package com.successacademy.academicservice.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingRuleDto {
    private Long id;
    private String grade;
    private BigDecimal minPercentage;
    private BigDecimal maxPercentage;
    private BigDecimal gradePoint;
    private String description;
    private boolean isPassing;
}
