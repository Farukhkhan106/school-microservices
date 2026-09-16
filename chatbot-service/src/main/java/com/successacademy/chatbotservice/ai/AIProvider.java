package com.successacademy.chatbotservice.ai;

import com.successacademy.chatbotservice.dto.ChatMessageDto;

import java.util.List;

public interface AIProvider {

    /**
     * Generates a response for the user given their role, identity context, history, and available ERP tools.
     */
    AIResponse chat(String userMessage,
                    String systemPrompt,
                    List<ChatMessageDto> history,
                    Long userId,
                    String role,
                    Long studentId,
                    Long teacherId);
}
