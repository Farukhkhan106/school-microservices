package com.successacademy.facultyservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicFacultyResponse {

    private Long id;
    private String name;
    private String designation;
    private String qualification;
    private int experience;
    private List<String> subjects;
    private String classTeacherOf;
    private String photoUrl;
    private String status;
}
