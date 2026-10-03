package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.MarkStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMarkResponse {
    private Long id;
    private Long scheduleId;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private BigDecimal marksObtained;
    private boolean isAbsent;
    private String grade;
    private BigDecimal gradePoint;
    private boolean isPassing;
    private String remarks;
    private MarkStatus status;
    private LocalDateTime updatedAt;
}
