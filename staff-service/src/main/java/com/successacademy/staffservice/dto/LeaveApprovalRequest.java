package com.successacademy.staffservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveApprovalRequest {

    private String action; // "APPROVE" or "REJECT"
    private String rejectionReason;
}
