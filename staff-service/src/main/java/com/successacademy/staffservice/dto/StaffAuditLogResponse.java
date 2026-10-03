package com.successacademy.staffservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAuditLogResponse {

    private Long id;
    private Long actorUserId;
    private Long staffId;
    private String action;
    private String oldValue;
    private String newValue;
    private String reason;
    private LocalDateTime timestamp;
}
