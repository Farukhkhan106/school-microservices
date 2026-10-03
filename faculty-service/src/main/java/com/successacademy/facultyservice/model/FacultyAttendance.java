package com.successacademy.facultyservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "faculty_attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_faculty_attendance_date", columnNames = {"faculty_id", "attendance_date"})
}, indexes = {
        @Index(name = "idx_faculty_att_date", columnList = "attendance_date"),
        @Index(name = "idx_faculty_att_faculty_id", columnList = "faculty_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    // PRESENT, ABSENT, HALF_DAY, LEAVE, HOLIDAY, WEEK_OFF
    @Column(nullable = false, length = 32)
    private String status;

    // ADMIN, LEAVE, SYSTEM, MANUAL
    @Column(length = 32)
    private String source;

    private LocalTime checkIn;

    private LocalTime checkOut;

    @Column(length = 500)
    private String remarks;

    private Long markedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
