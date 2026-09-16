package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TEMPORARY SUBSTITUTE / ABSENCE RECORD:
 * Records teacher absence on a specific date and binds temporary substitute coverage
 * for a specific class/section/period (or full day).
 *
 * Permanent Faculty.status, Faculty.classTeacherOf, TeacherClassAssignment, and
 * ClassSchedule records are NEVER modified or deleted by this entity.
 */
@Entity
@Table(
    name = "teacher_substitutes",
    indexes = {
        @Index(name = "idx_sub_date", columnList = "absence_date"),
        @Index(name = "idx_sub_absent_teacher", columnList = "absent_teacher_id"),
        @Index(name = "idx_sub_substitute_teacher", columnList = "substitute_teacher_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TeacherSubstitute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> faculty.id (the teacher who is absent)
    @Column(name = "absent_teacher_id", nullable = false)
    private Long absentTeacherId;

    // FK -> faculty.id (the substitute teacher covering; nullable if absent but not yet covered)
    @Column(name = "substitute_teacher_id")
    private Long substituteTeacherId;

    // The specific date of absence (e.g., 2026-09-08)
    @Column(name = "absence_date", nullable = false)
    private LocalDate date;

    @Column(name = "student_class", nullable = false)
    private String studentClass;   // e.g. "8"

    @Column(nullable = false)
    private String section;        // e.g. "A"

    @Column(nullable = false)
    private String subject;        // e.g. "Mathematics" or "General"

    // Nullable: null means full day / all periods; 1..12 means specific period
    @Column(name = "period_no")
    private Integer periodNo;

    // True if this substitute assignment delegates daily attendance authority for this class on this date
    @Column(name = "is_class_teacher_cover", nullable = false)
    @Builder.Default
    private boolean classTeacherCover = false;

    // Reason for absence: Sick Leave, Emergency, Official Duty, etc.
    @Column(length = 255)
    private String reason;

    // Status: "ABSENT" (uncovered), "ASSIGNED" (covered), "CANCELLED", "COMPLETED"
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "ASSIGNED";

    // Optional link to specific ClassSchedule slot ID
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
