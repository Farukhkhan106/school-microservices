package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "staff_attendance",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_staff_date",
        columnNames = {"staff_id", "attendance_date"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    // PRESENT, ABSENT, LATE, HALF_DAY, LEAVE, HOLIDAY, WEEK_OFF
    @Column(nullable = false, length = 20)
    private String status;

    private LocalTime checkIn;
    private LocalTime checkOut;

    // ADMIN, SELF, BIOMETRIC, RFID, IMPORT
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String source = "ADMIN";

    private Long markedBy; // actorUserId who marked or corrected it

    @Column(length = 300)
    private String remarks;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.source == null) this.source = "ADMIN";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
