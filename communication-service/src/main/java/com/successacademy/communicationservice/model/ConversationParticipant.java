package com.successacademy.communicationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_participants", indexes = {
        @Index(name = "idx_part_conv_user", columnList = "conversation_id, user_id", unique = true),
        @Index(name = "idx_part_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_role", nullable = false, length = 30)
    private String userRole; // ADMIN, TEACHER, STUDENT

    @Column(length = 100)
    private String username;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) joinedAt = LocalDateTime.now();
    }
}
