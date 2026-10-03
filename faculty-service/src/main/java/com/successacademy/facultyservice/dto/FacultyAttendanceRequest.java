package com.successacademy.facultyservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyAttendanceRequest {

    private Long facultyId;
    private LocalDate attendanceDate;
    private String status; // PRESENT, ABSENT, HALF_DAY, LEAVE, HOLIDAY, WEEK_OFF
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String remarks;
}
