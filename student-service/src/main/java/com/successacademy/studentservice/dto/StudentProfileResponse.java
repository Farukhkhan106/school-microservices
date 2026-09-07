package com.successacademy.studentservice.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * Full profile used by student portal (StudentProfile page).
 * Returns firstName and lastName separately so frontend can display them individually.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileResponse {

    // Identity
    private Long id;
    private String firstName;       // separate (not merged fullName)
    private String lastName;
    private String admissionNo;
    private String status;          // Active / Inactive
    private String photoUrl;

    // Academic
    private String studentClass;    // e.g. "10"
    private String section;         // e.g. "A"
    private String rollNo;
    private LocalDate admissionDate;

    // Personal
    private String gender;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;

    // Address
    private String address;
    private String city;
    private String state;
    private String pincode;

    // Father
    private String fatherName;
    private String fatherPhone;
    private String fatherOccupation;

    // Mother
    private String motherName;
    private String motherPhone;
    private String motherOccupation;

    // Guardian (optional)
    private String guardianName;
    private String guardianPhone;
    private String guardianRelation;
}
