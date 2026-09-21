package com.successacademy.communicationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "communication_audit_logs", indexes = {
        @Index(name = "idx_audit_time", columnList = "timestamp"),
        @Index(name = "idx_audit_performer", columnList = "performed_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunicationAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String action; // CREATE_CONVERSATION, SEND_MESSAGE, DELETE_MESSAGE, EDIT_MESSAGE, BROADCAST, MODERATION

    @Column(name = "performed_by", nullable = false)
    private Long performedBy;

    @Column(name = "performed_by_role", nullable = false, length = 30)
    private String performedByRole;

    @Column(name = "target_type", length = 50)
    private String targetType; // CONVERSATION, MESSAGE, BROADCAST

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
