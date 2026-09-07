package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ================= BASIC INFO =================
    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String photoUrl;        // NEW — profile photo URL

    // ================= ACADEMIC INFO =================
    @Column(unique = true)
    private String admissionNo;

    private String rollNo;
    private LocalDate admissionDate;
    private String status;          // Active / Inactive

    @Column(name = "class_name")
    private String studentClass;    // e.g. "10"

    private String section;         // NEW — e.g. "A"

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
}
