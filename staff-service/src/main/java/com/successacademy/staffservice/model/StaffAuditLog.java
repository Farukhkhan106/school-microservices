package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorUserId;

    private Long staffId;

    @Column(nullable = false, length = 100)
    private String action; // e.g. "STAFF_CREATED", "LOGIN_ENABLED", "STATUS_CHANGED"

    @Column(length = 1000)
    private String oldValue;

    @Column(length = 1000)
    private String newValue;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) this.timestamp = LocalDateTime.now();
    }
}
