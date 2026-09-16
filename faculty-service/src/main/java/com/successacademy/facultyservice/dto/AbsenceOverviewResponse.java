package com.successacademy.facultyservice.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AbsenceOverviewResponse {
    private LocalDate date;
    private int totalAbsentTeachers;
    private int totalPeriodsCovered;
    private int totalPeriodsUncovered;
    private List<SubstituteResponse> substitutions;
}
