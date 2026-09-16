package com.successacademy.chatbotservice.service;

import com.successacademy.chatbotservice.dto.ChatMessageDto;
import com.successacademy.chatbotservice.dto.ConversationDetailResponse;
import com.successacademy.chatbotservice.dto.ConversationSummaryResponse;
import com.successacademy.chatbotservice.model.ChatConversation;
import com.successacademy.chatbotservice.model.ChatMessage;

import java.util.List;

public interface ConversationService {

    ChatConversation getOrCreateConversation(Long conversationId, Long userId, String role, String initialMessage);

    List<ConversationSummaryResponse> getUserConversations(Long userId);

    ConversationDetailResponse getConversationDetail(Long conversationId, Long userId);

    void deleteConversation(Long conversationId, Long userId);

    List<ChatMessageDto> getRecentHistory(Long conversationId, int limit);

    ChatMessage saveMessage(Long conversationId, String sender, String message, String toolInvoked);
}
