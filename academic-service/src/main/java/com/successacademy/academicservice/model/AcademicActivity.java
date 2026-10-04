package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "academic_activities",
    indexes = {
        @Index(name = "idx_activity_teacher_status", columnList = "teacher_id, status"),
        @Index(name = "idx_activity_class_sec_status", columnList = "student_class, section, status"),
        @Index(name = "idx_activity_class_sec_subj", columnList = "student_class, section, subject"),
        @Index(name = "idx_activity_due_date", columnList = "due_date"),
        @Index(name = "idx_activity_type", columnList = "activity_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "student_class", nullable = false, length = 20)
    private String studentClass; // e.g. "8", "10"

    @Column(nullable = false, length = 10)
    private String section; // e.g. "A", "B"

    @Column(nullable = false, length = 100)
    private String subject; // e.g. "English", "Mathematics"

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "teacher_name", length = 100)
    private String teacherName;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    @Column(name = "passing_marks", precision = 5, scale = 2)
    private BigDecimal passingMarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ActivityStatus status = ActivityStatus.DRAFT;

    @Column(name = "is_graded", nullable = false)
    @Builder.Default
    private boolean graded = true;

    @Column(name = "submission_required", nullable = false)
    @Builder.Default
    private boolean submissionRequired = false;

    @Column(length = 2000)
    private String instructions;

    public boolean isSubmissionRequired() {
        if (submissionRequired) return true;
        return activityType == ActivityType.HOMEWORK ||
               activityType == ActivityType.ASSIGNMENT ||
               activityType == ActivityType.PROJECT ||
               activityType == ActivityType.WORKSHEET;
    }

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ActivityStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
