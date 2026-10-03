package com.successacademy.communicationservice.controller;

import com.successacademy.communicationservice.dto.*;
import com.successacademy.communicationservice.security.UserContext;
import com.successacademy.communicationservice.security.UserContextHolder;
import com.successacademy.communicationservice.service.CommunicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/communication")
@RequiredArgsConstructor
@Slf4j
public class CommunicationController {

    private final CommunicationService communicationService;

    // ── CONVERSATIONS LIST ───────────────────────────────────────
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations() {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(communicationService.getUserConversations(user));
    }

    // ── CONVERSATION DETAIL ──────────────────────────────────────
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDto> getConversation(@PathVariable Long id) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(communicationService.getConversationById(id, user));
    }

    // ── PAGINATED MESSAGE HISTORY ────────────────────────────────
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(communicationService.getConversationMessages(id, user, page, size));
    }

    // ── SEND MESSAGE ─────────────────────────────────────────────
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communicationService.sendMessage(id, user, request));
    }

    // ── CREATE CONVERSATION (DIRECT OR GROUP) ────────────────────
    @PostMapping("/conversations")
    public ResponseEntity<ConversationDto> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communicationService.createConversation(user, request));
    }

    // ── MARK CONVERSATION AS READ ────────────────────────────────
    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        UserContext user = UserContextHolder.require();
        communicationService.markAsRead(id, user);
        return ResponseEntity.noContent().build();
    }

    // ── UNREAD COUNTS ────────────────────────────────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(communicationService.getUnreadCount(user));
    }

    // ── ADMIN BROADCAST ──────────────────────────────────────────
    @PostMapping("/broadcast")
    public ResponseEntity<MessageDto> broadcast(@Valid @RequestBody BroadcastRequest request) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communicationService.broadcastAnnouncement(user, request));
    }

    // ── DELETE MESSAGE (SOFT DELETE) ─────────────────────────────
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        UserContext user = UserContextHolder.require();
        communicationService.deleteMessage(id, user);
        return ResponseEntity.noContent().build();
    }

    // ── ELIGIBLE RECIPIENTS FOR "NEW CONVERSATION" DIALOG ────────
    @GetMapping("/recipients")
    public ResponseEntity<List<RecipientDto>> getRecipients() {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(communicationService.getEligibleRecipients(user));
    }

    // ── SEARCH MESSAGES IN CONVERSATION ──────────────────────────
    @GetMapping("/conversations/{id}/search")
    public ResponseEntity<List<MessageDto>> searchMessages(
            @PathVariable Long id,
            @RequestParam String query) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(communicationService.searchMessages(id, query, user));
    }

    // ── HEALTH CHECK ─────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Communication Service is Healthy on Port 8091");
    }
}
