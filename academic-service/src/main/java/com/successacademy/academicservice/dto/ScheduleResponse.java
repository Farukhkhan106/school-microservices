package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.AssessmentComponent;
import com.successacademy.academicservice.model.MarkStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {
    private Long id;
    private Long assessmentId;
    private String assessmentName;
    private String studentClass;
    private String section;
    private String subject;
    private AssessmentComponent component;
    private String componentDisplayName;
    private LocalDate examDate;
    private Integer periodNo;
    private Long teacherId;
    private String teacherName;
    private BigDecimal maxMarks;
    private BigDecimal passMarks;
    private MarkStatus status;
    private int enrolledStudentsCount;
    private int enteredMarksCount;
}
