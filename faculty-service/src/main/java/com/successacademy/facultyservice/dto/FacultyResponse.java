package com.successacademy.facultyservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    private String facultyCode;
    private String department;
    private java.time.LocalDate joiningDate;
    private String employmentType;
    private java.math.BigDecimal baseSalary;
}
