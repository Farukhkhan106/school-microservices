package com.successacademy.feeservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fee_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FeeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;          // FK → student-service

    private String studentName;      // denormalized for display
    private String studentClass;     // e.g. "10-A"

    // Tuition | Transport | Library | Lab | Sports | Composite | Customized Fee Plan
    private String feeType;

    private BigDecimal amount;       // total fee due (from actual fee plan)
    private BigDecimal paidAmount;   // amount already paid

    private LocalDate dueDate;
    private LocalDate paymentDate;   // nullable until paid

    // Paid | Partial | Unpaid | Overdue
    private String status;

    // Cash | Online | Cheque | DD | UPI
    private String paymentMethod;    // nullable

    private String remarks;          // nullable

    @Column(name = "fee_plan_id")
    private Long feePlanId;          // Reference to StudentFeePlan (if generated from custom plan)

    @Column(name = "academic_year")
    private String academicYear;     // e.g. "2026-27"
}
