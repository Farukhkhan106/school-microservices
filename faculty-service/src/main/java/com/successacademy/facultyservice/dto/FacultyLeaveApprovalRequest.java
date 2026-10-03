package com.successacademy.facultyservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyLeaveApprovalRequest {

    private String action; // APPROVE or REJECT
    private String rejectionReason;
}
