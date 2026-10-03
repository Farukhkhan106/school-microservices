package com.successacademy.academicservice.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRankDto {
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String studentClass;
    private String section;
    private BigDecimal totalMarksObtained;
    private BigDecimal totalMaxMarks;
    private BigDecimal percentage;
    private String overallGrade;
    private String overallResult; // "PASSED", "FAILED"
    private Integer rankInSection;
    private Integer rankInClass;
}
