package com.successacademy.chatbotservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {

    private String message;
    private Long conversationId;
    private String role;
    private String toolUsed;
    private List<String> toolsInvoked;
    private List<ChatActionDto> actions;
    private LocalDateTime timestamp;
}
