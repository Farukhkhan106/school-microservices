package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftAssignmentRequest {

    private Long staffId;
    private Long shiftId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}
