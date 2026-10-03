package com.successacademy.facultyservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyBulkAttendanceRequest {

    private List<FacultyAttendanceRequest> items;
}
