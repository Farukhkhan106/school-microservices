package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "activity_submissions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_submission_activity_student",
        columnNames = {"activity_id", "student_id"}
    ),
    indexes = {
        @Index(name = "idx_submission_activity_student", columnList = "activity_id, student_id"),
        @Index(name = "idx_submission_student", columnList = "student_id"),
        @Index(name = "idx_submission_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivitySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_id", nullable = false)
    private Long activityId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", length = 100)
    private String studentName;

    @Column(name = "student_admission_no", length = 50)
    private String studentAdmissionNo;

    @Column(name = "submission_text", length = 5000)
    private String submissionText;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_size")
    private Long attachmentSize;

    @Column(name = "attachment_type", length = 100)
    private String attachmentType;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "teacher_feedback", length = 2000)
    private String teacherFeedback;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(name = "evaluated_by")
    private Long evaluatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SubmissionStatus.SUBMITTED;
        }
        if (this.attemptNumber == null) {
            this.attemptNumber = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
