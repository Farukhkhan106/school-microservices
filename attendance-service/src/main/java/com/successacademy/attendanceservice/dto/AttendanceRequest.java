package com.successacademy.attendanceservice.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * Single student attendance entry — used in bulk save list.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AttendanceRequest {

    private Long studentId;
    private String studentName;
    private String studentClass;
    private String section;
    private LocalDate date;
    private String status;      // PRESENT | ABSENT | LATE
    private String subject;     // optional
    private Long markedBy;      // teacherId
}
