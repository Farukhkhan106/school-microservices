package com.successacademy.galleryservice.service;

import com.successacademy.galleryservice.dto.GalleryItemRequest;
import com.successacademy.galleryservice.model.GalleryItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface GalleryService {

    // ── Public ───────────────────────────────────────────────────
    List<GalleryItem> getPublicItems();
    List<GalleryItem> getPublicItemsByCategory(String category);

    // ── Admin ────────────────────────────────────────────────────
    List<GalleryItem> getAllItems();
    GalleryItem getById(Long id);
    GalleryItem addItem(GalleryItemRequest request);
    GalleryItem updateItem(Long id, GalleryItemRequest request);
    void deleteItem(Long id);
    GalleryItem toggleActive(Long id, boolean active);
    GalleryItem togglePublic(Long id, boolean publicVisible);

    // ── File Upload ──────────────────────────────────────────────
    // Upload image file → saves to disk → returns GalleryItem with imageUrl set
    GalleryItem uploadImage(MultipartFile file, String title, String description,
                             String category, boolean publicVisible, String createdBy) throws IOException;

    List<GalleryItem> searchByTitle(String keyword);
}
