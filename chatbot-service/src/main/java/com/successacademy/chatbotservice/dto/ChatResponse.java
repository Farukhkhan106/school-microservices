package com.successacademy.chatbotservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private String message;
    private Long conversationId;
    private String role;
    private String toolUsed;
    private LocalDateTime timestamp;
}
