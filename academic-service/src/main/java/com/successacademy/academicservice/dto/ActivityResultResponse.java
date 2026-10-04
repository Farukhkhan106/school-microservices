package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.ResultStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResultResponse {
    private Long id;
    private Long activityId;
    private Long studentId;
    private String studentName;
    private String studentAdmissionNo;
    private BigDecimal obtainedMarks;
    private BigDecimal maxMarks;
    private BigDecimal percentage;
    private boolean isAbsent;
    private String grade;
    private String teacherFeedback;
    private ResultStatus status;
    private Long evaluatedBy;
    private LocalDateTime evaluatedAt;
}
