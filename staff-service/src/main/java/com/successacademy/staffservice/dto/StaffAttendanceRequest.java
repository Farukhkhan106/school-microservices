package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAttendanceRequest {

    private Long staffId;
    private LocalDate attendanceDate;
    private String status; // PRESENT, ABSENT, LATE, HALF_DAY, LEAVE
    private LocalTime checkIn;
    private LocalTime checkOut;
    private String source; // ADMIN, SELF, BIOMETRIC
    private String remarks;
}
