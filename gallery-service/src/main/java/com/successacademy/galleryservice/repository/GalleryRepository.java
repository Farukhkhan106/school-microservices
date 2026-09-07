package com.successacademy.galleryservice.repository;

import com.successacademy.galleryservice.model.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GalleryRepository extends JpaRepository<GalleryItem, Long> {

    // Public gallery — publicVisible=true AND active=true
    List<GalleryItem> findByPublicVisibleTrueAndActiveTrueOrderByDisplayOrderAscCreatedAtDesc();

    // Public + category filter
    List<GalleryItem> findByPublicVisibleTrueAndActiveTrueAndCategoryIgnoreCaseOrderByDisplayOrderAscCreatedAtDesc(String category);

    // Admin — all (ordered latest first)
    List<GalleryItem> findAllByOrderByCreatedAtDesc();

    // Category filter (admin)
    List<GalleryItem> findByCategoryIgnoreCaseOrderByCreatedAtDesc(String category);

    // Search by title
    List<GalleryItem> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
}
