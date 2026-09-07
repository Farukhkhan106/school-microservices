package com.successacademy.galleryservice.service;

import com.successacademy.galleryservice.dto.GalleryItemRequest;
import com.successacademy.galleryservice.model.GalleryItem;
import com.successacademy.galleryservice.repository.GalleryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GalleryServiceImpl implements GalleryService {

    private final GalleryRepository repository;

    @Value("${gallery.upload-dir:uploads/gallery}")
    private String uploadDir;

    // ── PUBLIC ───────────────────────────────────────────────────
    @Override
    public List<GalleryItem> getPublicItems() {
        return repository.findByPublicVisibleTrueAndActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
    }

    @Override
    public List<GalleryItem> getPublicItemsByCategory(String category) {
        return repository.findByPublicVisibleTrueAndActiveTrueAndCategoryIgnoreCaseOrderByDisplayOrderAscCreatedAtDesc(category);
    }

    // ── ADMIN ────────────────────────────────────────────────────
    @Override
    public List<GalleryItem> getAllItems() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public GalleryItem getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gallery item not found: " + id));
    }

    @Override
    public GalleryItem addItem(GalleryItemRequest request) {
        GalleryItem item = GalleryItem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(request.getCategory())
                .publicVisible(request.isPublicVisible())
                .active(true)
                .displayOrder(request.getDisplayOrder())
                .createdBy(request.getCreatedBy())
                .build();
        return repository.save(item);
    }

    @Override
    public GalleryItem updateItem(Long id, GalleryItemRequest request) {
        GalleryItem item = getById(id);
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            item.setImageUrl(request.getImageUrl());
        }
        item.setCategory(request.getCategory());
        item.setPublicVisible(request.isPublicVisible());
        item.setDisplayOrder(request.getDisplayOrder());
        return repository.save(item);
    }

    @Override
    public void deleteItem(Long id) {
        GalleryItem item = getById(id);
        // If it's an uploaded file, delete from disk too
        if (item.getImageUrl() != null && item.getImageUrl().startsWith("/gallery/uploads/")) {
            try {
                String filename = item.getImageUrl().replace("/gallery/uploads/", "");
                Path filePath = Paths.get(uploadDir).resolve(filename);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("Could not delete image file: {}", e.getMessage());
            }
        }
        repository.deleteById(id);
    }

    @Override
    public GalleryItem toggleActive(Long id, boolean active) {
        GalleryItem item = getById(id);
        item.setActive(active);
        return repository.save(item);
    }

    @Override
    public GalleryItem togglePublic(Long id, boolean publicVisible) {
        GalleryItem item = getById(id);
        item.setPublicVisible(publicVisible);
        return repository.save(item);
    }

    // ── FILE UPLOAD ──────────────────────────────────────────────
    @Override
    public GalleryItem uploadImage(MultipartFile file, String title, String description,
                                    String category, boolean publicVisible, String createdBy) throws IOException {
        // Create upload directory if not exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFilename = UUID.randomUUID().toString() + extension;

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // URL to access the file (served by this service)
        String imageUrl = "/gallery/uploads/" + uniqueFilename;

        // Create gallery item
        GalleryItem item = GalleryItem.builder()
                .title(title != null ? title : originalFilename)
                .description(description)
                .imageUrl(imageUrl)
                .category(category != null ? category : "Other")
                .publicVisible(publicVisible)
                .active(true)
                .displayOrder(0)
                .createdBy(createdBy)
                .build();

        return repository.save(item);
    }

    @Override
    public List<GalleryItem> searchByTitle(String keyword) {
        return repository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }
}
