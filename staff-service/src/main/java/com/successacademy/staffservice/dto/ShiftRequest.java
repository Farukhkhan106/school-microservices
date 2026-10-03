package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftRequest {

    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int graceMinutes;
    private String workingDays;
    private String status; // ACTIVE, INACTIVE
}
