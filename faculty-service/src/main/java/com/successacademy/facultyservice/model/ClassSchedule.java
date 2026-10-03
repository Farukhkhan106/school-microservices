package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule {

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
    private String dayOfWeek; // "Monday", "Tuesday", etc.

    @Column(nullable = false)
    private Integer periodNo; // 1 to 8

    private String startTime; // "08:00"

    private String endTime; // "08:45"
}
