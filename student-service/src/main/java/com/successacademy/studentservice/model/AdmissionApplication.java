package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "admission_applications",
    indexes = {
        @Index(name = "idx_adm_app_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_adm_app_tenant_class", columnList = "tenant_id, applying_class"),
        @Index(name = "idx_adm_app_tenant_session", columnList = "tenant_id, academic_session_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_adm_app_tenant_num", columnNames = {"tenant_id", "application_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "application_number", nullable = false, length = 50)
    private String applicationNumber;

    @Column(name = "academic_session_id", nullable = false)
    private Long academicSessionId;

    @Column(name = "academic_session_code", length = 50)
    private String academicSessionCode;

    @Column(name = "applying_class", nullable = false, length = 30)
    private String applyingClass;

    @Column(length = 50)
    private String stream;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AdmissionStatus status = AdmissionStatus.DRAFT;

    // Applicant Information
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(length = 50)
    private String nationality;

    @Column(length = 50)
    private String religion;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "alternate_phone", length = 30)
    private String alternatePhone;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String pincode;

    @Column(length = 50)
    @Builder.Default
    private String country = "India";

    // Academic History
    @Column(name = "previous_school_name", length = 150)
    private String previousSchoolName;

    @Column(name = "previous_school_class", length = 30)
    private String previousSchoolClass;

    @Column(name = "previous_school_percentage", length = 20)
    private String previousSchoolPercentage;

    @Column(name = "transfer_certificate_number", length = 100)
    private String transferCertificateNumber;

    @Column(name = "medical_conditions", length = 500)
    private String medicalConditions;

    @Column(name = "special_needs", length = 500)
    private String specialNeeds;

    @Column(length = 50)
    private String category; // General, OBC, SC, ST, etc.

    // Parent / Guardian in Application
    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "father_phone", length = 30)
    private String fatherPhone;

    @Column(name = "father_email", length = 150)
    private String fatherEmail;

    @Column(name = "father_occupation", length = 100)
    private String fatherOccupation;

    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "mother_phone", length = 30)
    private String motherPhone;

    @Column(name = "mother_email", length = 150)
    private String motherEmail;

    @Column(name = "mother_occupation", length = 100)
    private String motherOccupation;

    @Column(name = "guardian_name", length = 100)
    private String guardianName;

    @Column(name = "guardian_phone", length = 30)
    private String guardianPhone;

    @Column(name = "guardian_email", length = 150)
    private String guardianEmail;

    @Column(name = "guardian_relation", length = 50)
    private String guardianRelation;

    @Column(length = 50)
    @Builder.Default
    private String source = "ONLINE"; // ONLINE, WALK_IN, REFERRAL

    @Column(length = 1000)
    private String notes;

    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    @Column(name = "workflow_instance_id")
    private Long workflowInstanceId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "converted_by", length = 100)
    private String convertedBy;

    @Column(name = "converted_student_id")
    private Long convertedStudentId;

    @Column(name = "admission_number", length = 50)
    private String admissionNumber;

    @Version
    @Column(name = "opt_lock_version", nullable = false)
    @Builder.Default
    private Long optLockVersion = 0L;

    @Column(name = "created_by", length = 100)
    @Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by", length = 100)
    @Builder.Default
    private String updatedBy = "system";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper alias methods
    public Long getStudentId() {
        return this.convertedStudentId;
    }

    public void setStudentId(Long id) {
        this.convertedStudentId = id;
    }

    public String getApplicantFirstName() {
        return this.firstName;
    }

    public String getApplicantLastName() {
        return this.lastName;
    }

    public String getApplicantFullName() {
        return (this.firstName + " " + (this.lastName != null ? this.lastName : "")).trim();
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
        return this.notes;
    }

    @PrePersist
    protected void onCreate() {
        if (this.tenantId == null || this.tenantId.isBlank()) this.tenantId = "default";
        if (this.status == null) this.status = AdmissionStatus.DRAFT;
        if (this.optLockVersion == null) this.optLockVersion = 0L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
