package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_substitutes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSubstitute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long absentTeacherId;

    private String absentTeacherName;

    private Long substituteTeacherId;

    private String substituteTeacherName;

    @Column(name = "absence_date", nullable = false)
    private LocalDate date;

    private String studentClass;

    private String section;

    private String subject;

    private Integer periodNo;

    @Column(name = "is_class_teacher_cover")
    private boolean classTeacherCover;

    private String reason;

    @Column(nullable = false)
    private String status; // "ASSIGNED", "ABSENT", "CANCELLED"

    private Long scheduleId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = substituteTeacherId != null ? "ASSIGNED" : "ABSENT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
