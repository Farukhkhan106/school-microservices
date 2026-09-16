package com.successacademy.facultyservice.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SubstituteRequest {
    private Long absentTeacherId;
    private Long substituteTeacherId; // nullable if marking absent without substitute yet
    private LocalDate date;           // e.g. 2026-09-08
    private String studentClass;      // e.g. "8"
    private String section;           // e.g. "A"
    private String subject;           // e.g. "Mathematics" or "General"
    private Integer periodNo;         // nullable (null = full day)
    private Boolean classTeacherCover;// true if delegating class teacher attendance authority
    private String reason;            // e.g. "Sick Leave", "Emergency"
    private Long scheduleId;          // optional link to ClassSchedule id
}
