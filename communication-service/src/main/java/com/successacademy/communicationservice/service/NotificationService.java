package com.successacademy.communicationservice.service;

import com.successacademy.communicationservice.dto.CreateNotificationRequest;
import com.successacademy.communicationservice.dto.NotificationDto;
import com.successacademy.communicationservice.model.Notification;
import com.successacademy.communicationservice.repository.NotificationRepository;
import com.successacademy.communicationservice.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(UserContext user) {
        String tenantId = resolveTenant(user);
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, user.getUserId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotificationsPaged(UserContext user, int page, int size) {
        String tenantId = resolveTenant(user);
        return notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, user.getUserId(), PageRequest.of(page, size))
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UserContext user) {
        String tenantId = resolveTenant(user);
        return notificationRepository.countByTenantIdAndUserIdAndReadFalse(tenantId, user.getUserId());
    }

    @Transactional
    public NotificationDto markAsRead(Long id, UserContext user) {
        String tenantId = resolveTenant(user);
        Notification notif = notificationRepository.findByIdAndTenantIdAndUserId(id, tenantId, user.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notif.isRead()) {
            notif.setRead(true);
            notif.setReadAt(LocalDateTime.now());
            notif = notificationRepository.save(notif);
        }
        return toDto(notif);
    }

    @Transactional
    public int markAllAsRead(UserContext user) {
        String tenantId = resolveTenant(user);
        return notificationRepository.markAllAsRead(tenantId, user.getUserId(), LocalDateTime.now());
    }

    @Transactional
    public NotificationDto createNotification(String tenantId, CreateNotificationRequest req) {
        String actualTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        Notification notification = Notification.builder()
                .tenantId(actualTenant)
                .userId(req.getUserId())
                .username(req.getUsername())
                .userRole(req.getUserRole())
                .title(req.getTitle())
                .message(req.getMessage())
                .type(req.getType() != null ? req.getType().toUpperCase() : "GENERAL")
                .entityType(req.getEntityType())
                .entityId(req.getEntityId())
                .actionUrl(req.getActionUrl())
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = toDto(saved);

        // Try push to real-time websocket
        try {
            messagingTemplate.convertAndSend("/topic/user/" + saved.getUserId() + "/notifications", dto);
        } catch (Exception e) {
            log.debug("Could not push real-time notification to user {}: {}", saved.getUserId(), e.getMessage());
        }

        return dto;
    }

    private String resolveTenant(UserContext user) {
        return (user != null && user.getTenantId() != null && !user.getTenantId().isBlank())
                ? user.getTenantId()
                : "default";
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .username(n.getUsername())
                .userRole(n.getUserRole())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .actionUrl(n.getActionUrl())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
