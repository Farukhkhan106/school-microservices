package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.AssessmentComponent;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectResultDto {
    private String subject;
    private AssessmentComponent component;
    private BigDecimal marksObtained;
    private BigDecimal maxMarks;
    private BigDecimal passMarks;
    private String grade;
    private BigDecimal gradePoint;
    private boolean isPassing;
    private boolean isAbsent;
    private String remarks;
    private String teacherName;
}
