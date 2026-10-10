package com.successacademy.communicationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {
    private Long id;
    private Long userId;
    private String username;
    private String userRole;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private String entityType;
    private String entityId;
    private String actionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
