package com.successacademy.studentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLifecycleLogDto {
    private Long id;
    private Long studentId;
    private String tenantId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String reason;
    private String destinationInstitution;
    private String notes;
    private LocalDate effectiveDate;
    private String performedBy;
    private LocalDateTime performedAt;
}
