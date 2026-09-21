package com.successacademy.chatbotservice.controller;

import com.successacademy.chatbotservice.dto.ChatRequest;
import com.successacademy.chatbotservice.dto.ChatResponse;
import com.successacademy.chatbotservice.dto.ConversationDetailResponse;
import com.successacademy.chatbotservice.dto.ConversationSummaryResponse;
import com.successacademy.chatbotservice.security.UserContext;
import com.successacademy.chatbotservice.service.ChatbotService;
import com.successacademy.chatbotservice.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        ChatResponse response = chatbotService.processChat(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryResponse>> getConversations(HttpServletRequest httpRequest) {
        UserContext.requireAuthenticated(httpRequest);
        Long userId = UserContext.getUserId(httpRequest);
        List<ConversationSummaryResponse> list = conversationService.getUserConversations(userId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDetailResponse> getConversation(@PathVariable Long id, HttpServletRequest httpRequest) {
        UserContext.requireAuthenticated(httpRequest);
        Long userId = UserContext.getUserId(httpRequest);
        ConversationDetailResponse detail = conversationService.getConversationDetail(id, userId);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id, HttpServletRequest httpRequest) {
        UserContext.requireAuthenticated(httpRequest);
        Long userId = UserContext.getUserId(httpRequest);
        conversationService.deleteConversation(id, userId);
        return ResponseEntity.noContent().build();
    }
}
