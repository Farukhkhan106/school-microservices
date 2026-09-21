package com.successacademy.chatbotservice.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ContextManager {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationContext {
        private String lastClass;
        private String lastSection;
        private Long lastStudentId;
        private String lastStudentName;
        private Long lastTeacherId;
        private String lastTeacherName;
        private String lastSubject;
        private String lastDateWindow;
        private String lastDomain;
        private LocalDateTime lastUpdated;
    }

    private final Map<Long, ConversationContext> contextCache = new ConcurrentHashMap<>();

    public ConversationContext getContext(Long conversationId) {
        if (conversationId == null) return new ConversationContext();
        return contextCache.getOrDefault(conversationId, new ConversationContext());
    }

    public void updateContext(Long conversationId, ConversationContext update) {
        if (conversationId == null || update == null) return;
        ConversationContext existing = contextCache.computeIfAbsent(conversationId, k -> new ConversationContext());

        if (update.getLastClass() != null) existing.setLastClass(update.getLastClass());
        if (update.getLastSection() != null) existing.setLastSection(update.getLastSection());
        if (update.getLastStudentId() != null) existing.setLastStudentId(update.getLastStudentId());
        if (update.getLastStudentName() != null) existing.setLastStudentName(update.getLastStudentName());
        if (update.getLastTeacherId() != null) existing.setLastTeacherId(update.getLastTeacherId());
        if (update.getLastTeacherName() != null) existing.setLastTeacherName(update.getLastTeacherName());
        if (update.getLastSubject() != null) existing.setLastSubject(update.getLastSubject());
        if (update.getLastDateWindow() != null) existing.setLastDateWindow(update.getLastDateWindow());
        if (update.getLastDomain() != null) existing.setLastDomain(update.getLastDomain());
        existing.setLastUpdated(LocalDateTime.now());

        log.debug("Updated context for conversation [{}]: class={}-{}, student={}, teacher={}",
            conversationId, existing.getLastClass(), existing.getLastSection(), existing.getLastStudentName(), existing.getLastTeacherName());
    }

    public void clearContext(Long conversationId) {
        if (conversationId != null) {
            contextCache.remove(conversationId);
        }
    }
}
