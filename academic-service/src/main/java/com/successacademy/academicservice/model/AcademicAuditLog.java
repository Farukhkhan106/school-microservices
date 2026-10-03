package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "academic_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long actorUserId;

    @Column(length = 50)
    private String actorRole; // ADMIN, TEACHER

    @Column(nullable = false, length = 100)
    private String action; // e.g. "MARKS_DRAFT_SAVED", "MARKS_SUBMITTED", "MARKS_VERIFIED", "MARKS_CORRECTED", "RESULT_PUBLISHED"

    @Column(length = 50)
    private String targetEntity; // "AssessmentSchedule", "StudentMark", "Assessment"

    private Long targetId;

    @Column(length = 2000)
    private String oldValue;

    @Column(length = 2000)
    private String newValue;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
