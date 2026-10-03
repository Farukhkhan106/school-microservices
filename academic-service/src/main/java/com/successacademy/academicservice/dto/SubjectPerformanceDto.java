package com.successacademy.academicservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectPerformanceDto {
    private String subject;
    private BigDecimal averagePercentage;
    private BigDecimal highestMarks;
    private BigDecimal lowestMarks;
    private BigDecimal maxMarks;
    private int appearedCount;
    private int passedCount;
    private BigDecimal passPercentage;
}
