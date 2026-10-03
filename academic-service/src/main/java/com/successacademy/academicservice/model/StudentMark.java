package com.successacademy.academicservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_marks",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"schedule_id", "student_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @JsonIgnore
    private AssessmentSchedule schedule;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    private String studentName; // Denormalized for display
    private String rollNo;      // Denormalized for display

    @Column(precision = 5, scale = 2)
    private BigDecimal marksObtained; // Nullable if absent

    @Column(nullable = false)
    @Builder.Default
    private boolean isAbsent = false;

    @Column(length = 10)
    private String grade; // e.g. "A1", "B2", "E"

    @Column(precision = 4, scale = 2)
    private BigDecimal gradePoint;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPassing = false;

    @Column(length = 500)
    private String remarks;

    private Long enteredBy; // Teacher user ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MarkStatus status = MarkStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
