package com.successacademy.communicationservice.service;

import com.successacademy.communicationservice.dto.*;
import com.successacademy.communicationservice.security.UserContext;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommunicationService {

    List<ConversationDto> getUserConversations(UserContext user);

    ConversationDto getConversationById(Long conversationId, UserContext user);

    Page<MessageDto> getConversationMessages(Long conversationId, UserContext user, int page, int size);

    MessageDto sendMessage(Long conversationId, UserContext user, SendMessageRequest request);

    ConversationDto createConversation(UserContext user, CreateConversationRequest request);

    void markAsRead(Long conversationId, UserContext user);

    UnreadCountResponse getUnreadCount(UserContext user);

    MessageDto broadcastAnnouncement(UserContext user, BroadcastRequest request);

    void deleteMessage(Long messageId, UserContext user);

    List<RecipientDto> getEligibleRecipients(UserContext user);

    List<MessageDto> searchMessages(Long conversationId, String query, UserContext user);
}
