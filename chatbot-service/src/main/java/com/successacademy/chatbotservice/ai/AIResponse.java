package com.successacademy.chatbotservice.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIResponse {

    private String content;
    private String toolUsed;
    private boolean fallbackUsed;
}
