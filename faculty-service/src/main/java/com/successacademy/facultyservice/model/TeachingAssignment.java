package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_class_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long teacherId;

    private String teacherName;

    @Column(nullable = false)
    private String studentClass; // e.g. "10", "6", "Nursery"

    @Column(nullable = false)
    private String section; // e.g. "A", "B", "C"

    @Column(nullable = false)
    private String subject; // e.g. "Mathematics"

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
}
