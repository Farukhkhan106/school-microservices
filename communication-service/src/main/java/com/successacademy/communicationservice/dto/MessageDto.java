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
public class MessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderRole;
    private String senderUsername;
    private String content;
    private String messageType;
    private String attachmentUrl;
    private String attachmentName;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private boolean isDeleted;
}
