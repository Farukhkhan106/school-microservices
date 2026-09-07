package com.successacademy.galleryservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GalleryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    // URL to the image (external URL or /gallery/uploads/filename)
    @Column(nullable = false, length = 1000)
    private String imageUrl;

    // Sports | Academic | Cultural | Events | Campus | Other
    private String category;

    // true = visible on public gallery page
    private boolean publicVisible = true;

    // active toggle (admin can hide without deleting)
    private boolean active = true;

    // Sort order for display
    private int displayOrder = 0;

    private String createdBy;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
