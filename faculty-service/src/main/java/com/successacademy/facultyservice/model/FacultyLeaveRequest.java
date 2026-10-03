package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_leave_requests", indexes = {
        @Index(name = "idx_faculty_leave_faculty_id", columnList = "faculty_id"),
        @Index(name = "idx_faculty_leave_status", columnList = "status"),
        @Index(name = "idx_faculty_leave_dates", columnList = "from_date, to_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyLeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    // PAID, UNPAID, CASUAL, SICK, EMERGENCY, OTHER
    @Column(nullable = false, length = 32)
    private String leaveType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(length = 500)
    private String reason;

    // PENDING, APPROVED, REJECTED
    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING";

    private Long approvedBy;

    private LocalDateTime approvedAt;

    @Column(length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
