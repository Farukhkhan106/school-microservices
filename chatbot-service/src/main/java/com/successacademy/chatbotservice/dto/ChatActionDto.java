package com.successacademy.chatbotservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatActionDto {

    private String label;
    private String url;
    @Builder.Default
    private String type = "NAVIGATE"; // "NAVIGATE" | "EXTERNAL"
    @Builder.Default
    private boolean primary = false;
}
