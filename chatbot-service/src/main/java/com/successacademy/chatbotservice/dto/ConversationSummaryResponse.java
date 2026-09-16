package com.successacademy.chatbotservice.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryResponse {

    private Long id;
    private String title;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
