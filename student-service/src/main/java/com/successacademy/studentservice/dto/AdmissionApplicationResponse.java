package com.successacademy.studentservice.dto;

import com.successacademy.studentservice.model.AdmissionStatus;
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
public class AdmissionApplicationResponse {
    private Long id;
    private String tenantId;
    private String applicationNumber;
    private Long academicSessionId;
    private String sessionName;
    private String applyingClass;
    private String stream;

    private String applicantFirstName;
    private String applicantLastName;
    private String applicantFullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String nationality;
    private String religion;
    private String category;

    private String previousSchoolName;
    private String previousClass;
    private String previousPercentage;

    private String primaryContactName;
    private String primaryContactPhone;
    private String primaryContactEmail;
    private String primaryContactRelationship;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private AdmissionStatus status;
    private String source;
    private String remarks;
    private String reviewNotes;
    private String rejectionReason;

    private Long studentId;
    private String admissionNumber;

    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime convertedAt;
    private String convertedBy;
    private LocalDateTime rejectedAt;
    private String rejectedBy;

    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AdmissionDocumentResponse> documents;
}
