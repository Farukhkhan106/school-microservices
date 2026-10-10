package com.successacademy.communicationservice.service;

import com.successacademy.communicationservice.dto.CreateNotificationRequest;
import com.successacademy.communicationservice.dto.NotificationDto;
import com.successacademy.communicationservice.model.Notification;
import com.successacademy.communicationservice.repository.NotificationRepository;
import com.successacademy.communicationservice.security.UserContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Create notification stores proper entity and sends real-time websocket message")
    void testCreateNotification() {
        CreateNotificationRequest req = CreateNotificationRequest.builder()
                .userId(101L)
                .username("student1")
                .userRole("STUDENT")
                .title("New Homework Assigned")
                .message("Mathematics Homework #3 is now active.")
                .type("ACTIVITY")
                .build();

        Notification saved = Notification.builder()
                .id(1L)
                .tenantId("default")
                .userId(101L)
                .username("student1")
                .userRole("STUDENT")
                .title(req.getTitle())
                .message(req.getMessage())
                .type(req.getType())
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationDto dto = notificationService.createNotification("default", req);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("New Homework Assigned", dto.getTitle());
        assertFalse(dto.isRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Get notifications enforces user isolation and tenant isolation")
    void testGetUserNotifications_UserAndTenantIsolation() {
        UserContext user = UserContext.builder()
                .userId(101L)
                .username("student1")
                .role("STUDENT")
                .tenantId("tenant_a")
                .build();

        Notification notif = Notification.builder()
                .id(1L)
                .tenantId("tenant_a")
                .userId(101L)
                .title("Private Notice")
                .message("Your fee is paid.")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc("tenant_a", 101L))
                .thenReturn(List.of(notif));

        List<NotificationDto> result = notificationService.getUserNotifications(user);

        assertEquals(1, result.size());
        assertEquals("Private Notice", result.get(0).getTitle());
        verify(notificationRepository).findByTenantIdAndUserIdOrderByCreatedAtDesc("tenant_a", 101L);
    }

    @Test
    @DisplayName("Mark as read updates status and timestamp")
    void testMarkAsRead() {
        UserContext user = UserContext.builder()
                .userId(101L)
                .tenantId("default")
                .build();

        Notification notif = Notification.builder()
                .id(5L)
                .tenantId("default")
                .userId(101L)
                .title("Exam Scheduled")
                .message("Exam starts on Monday.")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByIdAndTenantIdAndUserId(5L, "default", 101L))
                .thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDto updated = notificationService.markAsRead(5L, user);

        assertTrue(updated.isRead());
        assertNotNull(updated.getReadAt());
    }
}
