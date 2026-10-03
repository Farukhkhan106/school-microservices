package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffStatusUpdateRequest {

    private String status; // ACTIVE, ON_NOTICE, SUSPENDED, RESIGNED, TERMINATED, RETIRED
    private LocalDate lastWorkingDate;
    private String reason;
}
