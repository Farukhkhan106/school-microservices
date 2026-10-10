package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.EnrollmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollmentResponse {
    private Long id;
    private String tenantId;
    private Long sessionId;
    private String sessionCode;
    private Long studentId;
    private String studentName;
    private String admissionNo;
    private String studentClass;
    private String section;
    private String rollNo;
    private EnrollmentStatus status;
    private LocalDate enrollmentDate;
    private LocalDate withdrawalDate;
    private String withdrawalReason;
    private String remarks;
    private LocalDateTime createdAt;
}
