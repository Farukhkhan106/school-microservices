package com.successacademy.noticeservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String content;

    private String category;    // Exam | Holiday | General | Event | Academic

    private String status;      // Published | Draft

    // Renamed from isPublic to publicVisible to avoid boolean getter naming conflict
    private boolean publicVisible;

    private boolean active = true;

    private LocalDate publishedAt;

    private LocalDate expiresAt;

    private String createdBy;

    private LocalDate startDate;
    private LocalDate endDate;
}
