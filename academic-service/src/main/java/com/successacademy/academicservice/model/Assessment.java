package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    private AcademicSession session;

    @Column(nullable = false, length = 150)
    private String name; // e.g. "Half-Yearly Examination 2025-26", "Unit Test 1"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AssessmentType assessmentType;

    @Column(length = 50)
    private String term; // e.g. "Term 1", "Term 2"

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grading_scheme_id")
    private GradingScheme gradingScheme;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRankVisible = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AssessmentStatus status = AssessmentStatus.SCHEDULED;

    private Long createdBy; // Admin user ID

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentSchedule> schedules = new ArrayList<>();

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
