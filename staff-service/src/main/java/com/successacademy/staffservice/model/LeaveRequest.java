package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long staffId;

    // CASUAL, SICK, PAID, UNPAID, EMERGENCY, OTHER
    @Column(nullable = false, length = 30)
    private String leaveType;

    @Column(nullable = false)
    private LocalDate fromDate;

    @Column(nullable = false)
    private LocalDate toDate;

    @Column(length = 500)
    private String reason;

    // PENDING, APPROVED, REJECTED, CANCELLED
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    private Long approvedBy; // actorUserId
    private LocalDateTime approvedAt;

    @Column(length = 300)
    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
