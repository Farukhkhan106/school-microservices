package com.successacademy.facultyservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyAttendanceResponse {

    private Long id;
    private Long facultyId;
    private String facultyName;
    private String facultyCode;
    private String department;
    private LocalDate attendanceDate;
    private String status; // PRESENT, ABSENT, HALF_DAY, LEAVE, HOLIDAY, WEEK_OFF
    private String source; // ADMIN, LEAVE, SYSTEM, MANUAL
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String remarks;
    private Long markedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
