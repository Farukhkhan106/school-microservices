package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_tenant", columnList = "tenant_id, class_name, section"),
        @Index(name = "idx_student_tenant_adm", columnList = "tenant_id, admission_no"),
        @Index(name = "idx_student_tenant_life", columnList = "tenant_id, lifecycle_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    @Builder.Default
    private String tenantId = "default";

    @Column(name = "admission_id")
    private Long admissionId; // FK → admission_applications.id if converted

    // ================= BASIC INFO =================
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String photoUrl;        // profile photo URL

    // ================= ACADEMIC INFO =================
    @Column(name = "admission_no", unique = true)
    private String admissionNo;

    @Column(name = "roll_no")
    private String rollNo;
    private LocalDate admissionDate;
    private String status;          // Active / Inactive (legacy compatibility)

    @Column(name = "lifecycle_status", length = 30)
    @Builder.Default
    private String lifecycleStatus = "ACTIVE"; // PROSPECT, APPLICANT, ADMITTED, ACTIVE, INACTIVE, TRANSFERRED, WITHDRAWN, GRADUATED, DECEASED

    @Column(name = "class_name")
    private String studentClass;    // e.g. "10"

    private String section;         // e.g. "A"

    // ================= ADDRESS =================
    private String address;
    private String city;
    private String state;           // NEW — e.g. "Madhya Pradesh"
    private String pincode;         // NEW — e.g. "455001"

    // ================= PARENT INFO =================
    @Column(nullable = false)
    private String fatherName;

    private String fatherPhone;
    private String fatherOccupation;

    private String motherName;
    private String motherPhone;
    private String motherOccupation;

    private String guardianName;        // NEW — optional guardian
    private String guardianPhone;       // NEW
    private String guardianRelation;    // NEW

    public LocalDate getDob() {
        return this.dateOfBirth;
    }

    public void setDob(LocalDate dob) {
        this.dateOfBirth = dob;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return null;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return null;
    }
}
