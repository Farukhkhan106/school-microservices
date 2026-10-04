package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "activity_results",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_result_activity_student",
        columnNames = {"activity_id", "student_id"}
    ),
    indexes = {
        @Index(name = "idx_result_activity_student", columnList = "activity_id, student_id"),
        @Index(name = "idx_result_student", columnList = "student_id"),
        @Index(name = "idx_result_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResult {

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

    @Column(name = "obtained_marks", precision = 5, scale = 2)
    private BigDecimal obtainedMarks;

    @Column(name = "max_marks", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "is_absent", nullable = false)
    @Builder.Default
    private boolean isAbsent = false;

    @Column(length = 10)
    private String grade;

    @Column(name = "teacher_feedback", length = 1000)
    private String teacherFeedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ResultStatus status = ResultStatus.DRAFT;

    @Column(name = "evaluated_by")
    private Long evaluatedBy;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

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
            this.status = ResultStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
