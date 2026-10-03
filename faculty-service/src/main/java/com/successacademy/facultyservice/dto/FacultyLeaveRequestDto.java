package com.successacademy.facultyservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyLeaveRequestDto {

    private Long facultyId;
    private String leaveType; // PAID, UNPAID, CASUAL, SICK, EMERGENCY, OTHER
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
}
