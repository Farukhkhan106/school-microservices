package com.successacademy.staffservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateSalaryRequest {

    private Integer month;
    private Integer year;
    private Long staffId; // optional, if null calculate for all active staff
    private Integer workingDays; // optional override (defaults to month working days, e.g. 26)
}
