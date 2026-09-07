package com.successacademy.studentservice.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * Used for admin list table and teacher student list.
 * Contains all fields needed by the frontend admin StudentsPage and TeacherStudents.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {

    private Long id;
    private String admissionNo;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String gender;
    private String studentClass;    // e.g. "10"
    private String section;         // e.g. "A"
    private String rollNo;
    private String status;          // Active / Inactive
    private String photoUrl;

    // Parent info for admin list
    private String fatherName;
    private String fatherPhone;
}
