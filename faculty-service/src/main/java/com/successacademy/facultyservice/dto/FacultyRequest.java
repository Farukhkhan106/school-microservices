package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FacultyRequest {

    private String name;
    private String email;
    private String phone;
    private String designation;
    private String qualification;
    private int experience;
    private String subjects;        // comma-separated
    private String classTeacherOf;  // nullable
    private String photoUrl;
    private String status;
    private Long userId;            // nullable — links to auth user
}
