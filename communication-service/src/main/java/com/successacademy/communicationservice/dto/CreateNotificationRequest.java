package com.successacademy.communicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
    @NotNull(message = "Recipient userId is required")
    private Long userId;

    private String username;

    private String userRole;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String type; // ACADEMIC, ATTENDANCE, FEE, EXAM, ACTIVITY, SYSTEM, GENERAL

    private String entityType;

    private String entityId;

    private String actionUrl;
}
