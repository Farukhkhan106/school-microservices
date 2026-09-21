package com.successacademy.communicationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_msg_conv", columnList = "conversation_id"),
        @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_role", nullable = false, length = 30)
    private String senderRole; // ADMIN, TEACHER, STUDENT

    @Column(name = "sender_username", nullable = false, length = 100)
    private String senderUsername;

    @Column(nullable = false, length = 4000)
    private String content;

    // TEXT, ANNOUNCEMENT, FILE
    @Column(name = "message_type", nullable = false, length = 30)
    @Builder.Default
    private String messageType = "TEXT";

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_name", length = 200)
    private String attachmentName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
