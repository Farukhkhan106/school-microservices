package com.successacademy.studentservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentRequest {

    // BASIC
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String photoUrl;            // profile photo URL

    // ACADEMIC
    private String admissionNo;
    private String rollNo;
    private LocalDate admissionDate;
    private String status;              // Active / Inactive
    private String studentClass;        // e.g. "10"
    private String section;             // e.g. "A"

    // ADDRESS
    private String address;
    private String city;
    private String state;               // e.g. "Madhya Pradesh"
    private String pincode;             // e.g. "455001"

    // PARENTS
    private String fatherName;
    private String fatherPhone;
    private String fatherOccupation;

    private String motherName;
    private String motherPhone;
    private String motherOccupation;

    // GUARDIAN (optional)
    private String guardianName;
    private String guardianPhone;
    private String guardianRelation;
}
