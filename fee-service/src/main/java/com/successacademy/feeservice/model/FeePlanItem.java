package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "fee_plan_items",
    indexes = {
        @Index(name = "idx_plan_item_plan_id", columnList = "fee_plan_id")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeePlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fee_plan_id", nullable = false)
    private Long feePlanId;

    @Column(name = "fee_head", nullable = false, length = 64)
    private String feeHead;          // Tuition, Transport, Lab, Library, Books, Uniform, etc.

    @Column(name = "standard_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardAmount;

    @Column(name = "applicable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal applicableAmount;

    @Column(nullable = false)
    private Boolean mandatory;

    @Column(nullable = false)
    private Boolean enabled;          // true = opted in, false = opted out
}
