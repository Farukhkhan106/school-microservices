package com.successacademy.chatbotservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_conversation_id", columnList = "conversation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 32)
    private String sender; // USER, ASSISTANT, SYSTEM

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "tool_invoked", length = 128)
    private String toolInvoked; // e.g. "getMyAttendance", "getFeeSummary", etc.

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
