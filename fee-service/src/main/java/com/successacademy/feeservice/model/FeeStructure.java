package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_structure")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeeStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String className;        // e.g. "Class 1", "Class 10", "Nursery", "LKG", "UKG"

    private BigDecimal tuitionFee;
    private BigDecimal transportFee;
    private BigDecimal libraryFee;
    private BigDecimal labFee;
    private BigDecimal sportsFee;
    private BigDecimal booksFee;
    private BigDecimal uniformFee;
    private BigDecimal examFee;
    private BigDecimal otherFee;

    private BigDecimal totalFee;     // sum of all above
}
