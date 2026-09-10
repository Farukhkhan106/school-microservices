package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduleRequest {
    private String dayOfWeek;      // Monday..Sunday
    private int periodNo;          // 1..12
    private String startTime;      // "08:00"
    private String endTime;        // "08:45"
    private String studentClass;
    private String section;
    private String subject;
    private Long teacherId;
}