package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignmentResponse {

    private Long id;
    private Long staffId;
    private String staffName;
    private String staffCode;
    private Long shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
