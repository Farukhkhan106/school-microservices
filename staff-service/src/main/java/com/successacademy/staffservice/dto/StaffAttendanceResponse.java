package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAttendanceResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private String departmentName;
    private String designationName;
    private LocalDate attendanceDate;
    private String status;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String source;
    private Long markedBy;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
