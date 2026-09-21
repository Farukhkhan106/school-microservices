package com.successacademy.communicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDto {
    private Long id;
    private String type; // DIRECT, CLASS_GROUP, STAFF_GROUP, BROADCAST
    private String title;
    private String targetClass;
    private String targetSection;
    private String targetRole;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

    private long unreadCount;
    private MessageDto lastMessage;

    // For DIRECT conversations:
    private Long otherUserId;
    private String otherUsername;
    private String otherUserRole;
    private String otherDisplayName;
}
