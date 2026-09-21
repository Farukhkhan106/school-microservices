package com.successacademy.communicationservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_type", columnList = "type"),
        @Index(name = "idx_conv_class_sec", columnList = "target_class, target_section")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DIRECT, CLASS_GROUP, STAFF_GROUP, BROADCAST
    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 150)
    private String title;

    // For CLASS_GROUP: e.g. "10" and "A"
    @Column(name = "target_class", length = 30)
    private String targetClass;

    @Column(name = "target_section", length = 30)
    private String targetSection;

    // Optional target role filter for BROADCAST (e.g. "ALL", "TEACHER", "STUDENT")
    @Column(name = "target_role", length = 30)
    private String targetRole;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
