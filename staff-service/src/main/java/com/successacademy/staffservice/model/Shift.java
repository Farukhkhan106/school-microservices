package com.successacademy.staffservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name; // e.g. "Morning Shift", "General Day Shift"

    @Column(nullable = false)
    private LocalTime startTime; // e.g. 07:00

    @Column(nullable = false)
    private LocalTime endTime; // e.g. 15:30

    @Builder.Default
    private int graceMinutes = 15;

    @Column(length = 100)
    @Builder.Default
    private String workingDays = "Mon,Tue,Wed,Thu,Fri,Sat";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
