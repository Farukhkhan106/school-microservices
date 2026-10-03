package com.successacademy.facultyservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyLeaveResponseDto {

    private Long id;
    private Long facultyId;
    private String facultyName;
    private String facultyCode;
    private String department;
    private String designation;
    private String leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
