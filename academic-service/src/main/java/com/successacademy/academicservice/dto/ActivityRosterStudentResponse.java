package com.successacademy.academicservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRosterStudentResponse {
    private Long studentId;
    private String admissionNo;
    private String studentName;
    private String studentClass;
    private String section;
    private String rollNo;
    private SubmissionResponse submission;
    private ActivityResultResponse result;
}
