package com.successacademy.studentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionApplicationRequest {

    @NotNull(message = "Academic session ID is required")
    private Long academicSessionId;

    private String academicSessionCode;

    @NotBlank(message = "Applying class is required")
    private String applyingClass;

    private String stream;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String nationality;
    private String religion;
    private String email;
    private String phone;
    private String alternatePhone;

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;

    private String previousSchoolName;
    private String previousSchoolClass;
    private String previousSchoolPercentage;
    private String transferCertificateNumber;

    private String medicalConditions;
    private String specialNeeds;
    private String category;

    // Parent information
    private String fatherName;
    private String fatherPhone;
    private String fatherEmail;
    private String fatherOccupation;

    private String motherName;
    private String motherPhone;
    private String motherEmail;
    private String motherOccupation;

    private String guardianName;
    private String guardianPhone;
    private String guardianEmail;
    private String guardianRelation;

    private String source;
    private String notes;
    private String remarks;

    // For updates: optimistic locking
    private Long optLockVersion;

    public String getApplicantFirstName() {
        return this.firstName;
    }

    public String getApplicantLastName() {
        return this.lastName;
    }

    public String getPrimaryContactName() {
        if (this.guardianName != null && !this.guardianName.isBlank()) return this.guardianName;
        if (this.fatherName != null && !this.fatherName.isBlank()) return this.fatherName;
        if (this.motherName != null && !this.motherName.isBlank()) return this.motherName;
        return this.firstName + " Contact";
    }

    public String getPrimaryContactPhone() {
        if (this.guardianPhone != null && !this.guardianPhone.isBlank()) return this.guardianPhone;
        if (this.fatherPhone != null && !this.fatherPhone.isBlank()) return this.fatherPhone;
        if (this.motherPhone != null && !this.motherPhone.isBlank()) return this.motherPhone;
        return this.phone != null ? this.phone : "";
    }

    public String getPrimaryContactEmail() {
        if (this.guardianEmail != null && !this.guardianEmail.isBlank()) return this.guardianEmail;
        if (this.fatherEmail != null && !this.fatherEmail.isBlank()) return this.fatherEmail;
        if (this.motherEmail != null && !this.motherEmail.isBlank()) return this.motherEmail;
        return this.email;
    }

    public String getPrimaryContactRelationship() {
        if (this.guardianRelation != null && !this.guardianRelation.isBlank()) return this.guardianRelation;
        if (this.fatherName != null && !this.fatherName.isBlank()) return "FATHER";
        if (this.motherName != null && !this.motherName.isBlank()) return "MOTHER";
        return "GUARDIAN";
    }

    public String getPreviousClass() {
        return this.previousSchoolClass;
    }

    public String getPreviousPercentage() {
        return this.previousSchoolPercentage;
    }

    public String getPostalCode() {
        return this.pincode;
    }

    public String getRemarks() {
        return (this.remarks != null && !this.remarks.isBlank()) ? this.remarks : this.notes;
    }
}
