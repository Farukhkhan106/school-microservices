package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyCalculateSalaryRequest {

    private Integer month;
    private Integer year;
    private Integer workingDays; // optional default 26
    private Long facultyId;      // optional: calculate for single faculty or all
}
