package com.successacademy.facultyservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SubstituteResponse {
    private Long id;
    private Long absentTeacherId;
    private String absentTeacherName;
    private Long substituteTeacherId;
    private String substituteTeacherName;
    private LocalDate date;
    private String studentClass;
    private String section;
    private String subject;
    private Integer periodNo;
    private boolean classTeacherCover;
    private String reason;
    private String status;
    private Long scheduleId;
    private LocalDateTime createdAt;
}
