package com.successacademy.chatbotservice.ai;

import com.successacademy.chatbotservice.dto.ChatActionDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIResponse {

    private String content;
    private String toolUsed;
    private List<String> toolsInvoked;
    private List<ChatActionDto> actions;
    private boolean fallbackUsed;
}
