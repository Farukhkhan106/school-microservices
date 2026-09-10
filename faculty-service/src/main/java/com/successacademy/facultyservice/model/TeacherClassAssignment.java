package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * SUBJECT-TEACHER assignment: "teacher X teaches SUBJECT to CLASS-SECTION".
 * A teacher can have many of these (e.g. Mathematics in 9-A and 10-A) —
 * this is a different concept from Faculty.classTeacherOf (one class only).
 */
@Entity
@Table(
    name = "teacher_class_assignments",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"teacher_id", "student_class", "section", "subject"}
    )
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TeacherClassAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK → faculty.id (no JPA relation across services — plain id link)
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "student_class", nullable = false)
    private String studentClass;   // e.g. "8"

    @Column(nullable = false)
    private String section;        // e.g. "A"

    @Column(nullable = false)
    private String subject;        // e.g. "Mathematics"

    private String status;         // Active | Inactive
}