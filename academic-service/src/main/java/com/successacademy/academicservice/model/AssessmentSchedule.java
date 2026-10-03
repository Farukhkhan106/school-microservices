package com.successacademy.academicservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "assessment_schedules",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"assessment_id", "student_class", "section", "subject", "component"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    @JsonIgnore
    private Assessment assessment;

    @Column(name = "student_class", nullable = false, length = 20)
    private String studentClass; // e.g. "10", "9"

    @Column(nullable = false, length = 10)
    private String section; // e.g. "A", "B"

    @Column(nullable = false, length = 100)
    private String subject; // e.g. "Mathematics"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AssessmentComponent component = AssessmentComponent.THEORY;

    @Column(nullable = false)
    private LocalDate examDate;

    private Integer periodNo; // 1 to 8, or null for general exam

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId; // Faculty ID authorized to enter marks

    private String teacherName; // Denormalized for display

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxMarks; // e.g. 80.00 or 20.00

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal passMarks; // e.g. 27.00 or 7.00

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MarkStatus status = MarkStatus.DRAFT;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentMark> marks = new ArrayList<>();

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
