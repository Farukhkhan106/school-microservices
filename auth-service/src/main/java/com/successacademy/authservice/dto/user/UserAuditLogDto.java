package com.successacademy.authservice.dto.user;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLogDto {

    private Long id;
    private String tenantId;
    private Long actorUserId;
    private String actorUsername;
    private String actorRole;
    private Long targetUserId;
    private String targetUsername;
    private String action;
    private String details;
    private LocalDateTime timestamp;
}
