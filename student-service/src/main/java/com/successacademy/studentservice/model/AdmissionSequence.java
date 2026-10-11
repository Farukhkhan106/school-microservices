package com.successacademy.studentservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "admission_sequences",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_seq_tenant_type_year", columnNames = {"tenant_id", "sequence_type", "sequence_year"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "sequence_type", nullable = false, length = 50)
    private String sequenceType; // APPLICATION, ADMISSION

    @Column(name = "sequence_year", nullable = false)
    private Integer sequenceYear;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Long currentValue = 0L;
}
