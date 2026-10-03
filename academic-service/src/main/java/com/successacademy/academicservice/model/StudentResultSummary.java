package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "student_result_summaries",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"assessment_id", "student_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResultSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "assessment_id", nullable = false)
    private Long assessmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    private String studentName;
    private String rollNo;

    @Column(name = "student_class", nullable = false, length = 20)
    private String studentClass;

    @Column(nullable = false, length = 10)
    private String section;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal totalMarksObtained;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal totalMaxMarks;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(length = 10)
    private String overallGrade; // e.g. "A1", "B2"

    @Column(length = 30)
    private String overallResult; // "PASSED", "FAILED", "COMPARTMENT"

    private Integer rankInSection;
    private Integer rankInClass;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.calculatedAt = LocalDateTime.now();
    }
}
