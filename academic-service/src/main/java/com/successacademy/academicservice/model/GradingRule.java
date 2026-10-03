package com.successacademy.academicservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "grading_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id", nullable = false)
    @JsonIgnore
    private GradingScheme scheme;

    @Column(nullable = false, length = 10)
    private String grade; // e.g. "A1", "A2", "B1", "B2", "C1", "C2", "D", "E"

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal minPercentage; // e.g. 91.00

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal maxPercentage; // e.g. 100.00

    @Column(precision = 4, scale = 2)
    private BigDecimal gradePoint; // e.g. 10.0

    private String description; // e.g. "Outstanding", "Excellent"

    @Column(nullable = false)
    private boolean isPassing; // true for A1-D, false for E
}
