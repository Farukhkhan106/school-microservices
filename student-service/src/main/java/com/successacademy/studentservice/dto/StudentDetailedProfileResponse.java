package com.successacademy.studentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailedProfileResponse {
    private Long id;
    private String tenantId;
    private String firstName;
    private String lastName;
    private String admissionNo;
    private String rollNo;
    private String studentClass;
    private String section;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dob;
    private String address;
    private String status;
    private String lifecycleStatus;
    private Long admissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Enriched Sections
    private List<StudentGuardianDto> guardians;
    private List<StudentLifecycleLogDto> lifecycleHistory;
    private AdmissionApplicationResponse admissionApplication;
}
