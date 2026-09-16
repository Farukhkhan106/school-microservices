package com.successacademy.facultyservice.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FacultyResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String designation;
    private String qualification;
    private int experience;
    private List<String> subjects;  // split from comma-separated string
    private String classTeacherOf;
    private String photoUrl;
    private String status;
    private Long userId;
    private List<String> assignedClasses;
    private int todayClassesCount;
}
