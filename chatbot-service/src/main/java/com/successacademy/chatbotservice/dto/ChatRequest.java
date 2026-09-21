package com.successacademy.chatbotservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 1000, message = "Message exceeds maximum length of 1000 characters")
    private String message;

    private Long conversationId;
}
