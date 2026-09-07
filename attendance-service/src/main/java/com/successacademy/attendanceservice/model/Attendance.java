package com.successacademy.attendanceservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"student_id", "attendance_date", "subject"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    private String studentName;     // denormalized for display

    @Column(name = "student_class", nullable = false)
    private String studentClass;    // e.g. "10"

    private String section;         // e.g. "A"

    @Column(name = "attendance_date", nullable = false)
    private LocalDate date;

    // PRESENT | ABSENT | LATE
    @Column(nullable = false)
    private String status;

    // optional — for per-subject attendance
    private String subject;

    // teacherId who marked this attendance
    private Long markedBy;
}
