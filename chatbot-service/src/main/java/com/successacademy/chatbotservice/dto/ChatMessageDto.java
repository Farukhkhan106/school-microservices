package com.successacademy.chatbotservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long id;
    private String sender;
    private String message;
    private String toolInvoked;
    private LocalDateTime createdAt;
}
