package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequestDto {

    private Long staffId; // optional if self request
    private String leaveType; // CASUAL, SICK, PAID, UNPAID, EMERGENCY, OTHER
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
}
