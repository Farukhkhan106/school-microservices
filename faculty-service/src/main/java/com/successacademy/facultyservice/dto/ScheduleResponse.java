package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScheduleResponse {
    private Long id;
    private String dayOfWeek;
    private int periodNo;
    private String startTime;
    private String endTime;
    private String studentClass;
    private String section;
    private String subject;
    private Long teacherId;
    private String teacherName;    // denormalized for display

    public String getClassSection() {
        return studentClass + "-" + section;
    }
}