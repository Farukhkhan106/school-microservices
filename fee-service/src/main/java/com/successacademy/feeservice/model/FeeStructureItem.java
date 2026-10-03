package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_structure_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeeStructureItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(nullable = false, length = 64)
    private String feeHead;          // e.g. "Tuition", "Transport", "Lab", "Library", "Books", "Uniform", "Exam", "Sports", "Activity", "Other"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Boolean mandatory;       // true = Mandatory, false = Optional

    @Column(nullable = false)
    private Boolean active;          // true = Active in template
}
