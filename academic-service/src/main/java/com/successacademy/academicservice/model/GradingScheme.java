package com.successacademy.academicservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grading_schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g. "CBSE 9-Point Scale", "Primary 5-Point Scale"

    private String description;

    @Column(nullable = false)
    private boolean isDefault;

    @OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("minPercentage DESC")
    @Builder.Default
    private List<GradingRule> rules = new ArrayList<>();
}
