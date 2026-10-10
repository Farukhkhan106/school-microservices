package com.successacademy.academicservice.dto;

import com.successacademy.academicservice.model.EnrollmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrollmentRequest {
    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    private String studentName;
    private String admissionNo;

    @NotBlank(message = "Student class is required")
    private String studentClass;

    @NotBlank(message = "Section is required")
    private String section;

    private String rollNo;
    private EnrollmentStatus status;
    private LocalDate enrollmentDate;
    private String remarks;
}
