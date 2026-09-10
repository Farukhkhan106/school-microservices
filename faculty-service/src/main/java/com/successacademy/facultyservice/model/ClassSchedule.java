package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * CLASS TIMETABLE SLOT: one class-section has AT MOST one period per
 * day+periodNo (unique constraint), and one teacher cannot be in two
 * classes at the same time (enforced in the service layer).
 */
@Entity
@Table(
    name = "class_schedule",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"day_of_week", "period_no", "student_class", "section"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClassSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Monday..Sunday (normalized)
    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek;

    @Column(name = "period_no", nullable = false)
    private int periodNo;          // 1..12

    private String startTime;      // "08:00"
    private String endTime;        // "08:45"

    @Column(name = "student_class", nullable = false)
    private String studentClass;   // e.g. "8"

    @Column(nullable = false)
    private String section;        // e.g. "A"

    @Column(nullable = false)
    private String subject;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;        // FK → faculty.id
}