package com.successacademy.communicationservice.controller;

import com.successacademy.communicationservice.dto.CreateNotificationRequest;
import com.successacademy.communicationservice.dto.NotificationDto;
import com.successacademy.communicationservice.security.UserContext;
import com.successacademy.communicationservice.security.UserContextHolder;
import com.successacademy.communicationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/communication/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ── GET USER NOTIFICATIONS ───────────────────────────────────
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications() {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    // ── PAGINATED NOTIFICATIONS ──────────────────────────────────
    @GetMapping("/paged")
    public ResponseEntity<Page<NotificationDto>> getNotificationsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(notificationService.getUserNotificationsPaged(user, page, size));
    }

    // ── UNREAD COUNT ─────────────────────────────────────────────
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        UserContext user = UserContextHolder.require();
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    // ── MARK SINGLE AS READ ──────────────────────────────────────
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(@PathVariable Long id) {
        UserContext user = UserContextHolder.require();
        return ResponseEntity.ok(notificationService.markAsRead(id, user));
    }

    // ── MARK ALL AS READ ─────────────────────────────────────────
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        UserContext user = UserContextHolder.require();
        int updated = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true, "count", updated));
    }

    // ── CREATE NOTIFICATION (SERVICE OR ADMIN) ───────────────────
    @PostMapping
    public ResponseEntity<NotificationDto> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        NotificationDto created = notificationService.createNotification(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
