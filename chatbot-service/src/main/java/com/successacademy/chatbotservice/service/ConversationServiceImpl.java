package com.successacademy.chatbotservice.service;

import com.successacademy.chatbotservice.dto.ChatMessageDto;
import com.successacademy.chatbotservice.dto.ConversationDetailResponse;
import com.successacademy.chatbotservice.dto.ConversationSummaryResponse;
import com.successacademy.chatbotservice.model.ChatConversation;
import com.successacademy.chatbotservice.model.ChatMessage;
import com.successacademy.chatbotservice.repository.ChatConversationRepository;
import com.successacademy.chatbotservice.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    @Override
    @Transactional
    public ChatConversation getOrCreateConversation(Long conversationId, Long userId, String role, String initialMessage) {
        if (conversationId != null) {
            ChatConversation existing = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + conversationId));

            if (!existing.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this conversation.");
            }
            return existing;
        }

        // Create new conversation
        String title = initialMessage != null && !initialMessage.isBlank()
            ? (initialMessage.length() > 50 ? initialMessage.substring(0, 47) + "..." : initialMessage)
            : "New Chat";

        ChatConversation newConv = ChatConversation.builder()
            .userId(userId)
            .role(role != null ? role.toUpperCase() : "STUDENT")
            .title(title)
            .build();

        return conversationRepository.save(newConv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)
            .stream()
            .map(c -> ConversationSummaryResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .role(c.getRole())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(Long conversationId, Long userId) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + conversationId));

        if (!conv.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this conversation.");
        }

        List<ChatMessageDto> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
            .stream()
            .map(m -> ChatMessageDto.builder()
                .id(m.getId())
                .sender(m.getSender())
                .message(m.getMessage())
                .toolInvoked(m.getToolInvoked())
                .createdAt(m.getCreatedAt())
                .build())
            .collect(Collectors.toList());

        return ConversationDetailResponse.builder()
            .id(conv.getId())
            .title(conv.getTitle())
            .role(conv.getRole())
            .createdAt(conv.getCreatedAt())
            .updatedAt(conv.getUpdatedAt())
            .messages(messages)
            .build();
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        ChatConversation conv = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found: " + conversationId));

        if (!conv.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this conversation.");
        }

        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentHistory(Long conversationId, int limit) {
        List<ChatMessage> list = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        int start = Math.max(0, list.size() - limit);
        return list.subList(start, list.size()).stream()
            .map(m -> ChatMessageDto.builder()
                .id(m.getId())
                .sender(m.getSender())
                .message(m.getMessage())
                .toolInvoked(m.getToolInvoked())
                .createdAt(m.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMessage saveMessage(Long conversationId, String sender, String message, String toolInvoked) {
        ChatMessage msg = ChatMessage.builder()
            .conversationId(conversationId)
            .sender(sender)
            .message(message)
            .toolInvoked(toolInvoked)
            .build();
        return messageRepository.save(msg);
    }
}
