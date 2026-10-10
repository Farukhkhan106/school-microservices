package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.SubmissionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionResponse {
    private Long id;
    private Long activityId;
    private Long studentId;
    private String studentName;
    private String studentAdmissionNo;
    private String submissionText;
    private String attachmentUrl;
    private String attachmentName;
    private Long attachmentSize;
    private String attachmentType;
    private LocalDateTime submittedAt;
    private SubmissionStatus status;
    private Integer attemptNumber;
    private String teacherFeedback;
    private LocalDateTime evaluatedAt;
    private Long evaluatedBy;
}
