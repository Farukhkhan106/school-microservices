package com.successacademy.chatbotservice.ai;

import com.successacademy.chatbotservice.dto.ChatMessageDto;

import java.util.List;

public interface AIProvider {

    AIResponse chat(String userMessage,
                    String systemPrompt,
                    List<ChatMessageDto> history,
                    Long userId,
                    String role,
                    Long studentId,
                    Long teacherId,
                    Long conversationId);

    default AIResponse chat(String userMessage,
                            String systemPrompt,
                            List<ChatMessageDto> history,
                            Long userId,
                            String role,
                            Long studentId,
                            Long teacherId) {
        return chat(userMessage, systemPrompt, history, userId, role, studentId, teacherId, null);
    }
}
