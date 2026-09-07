package com.successacademy.galleryservice.controller;

import com.successacademy.galleryservice.dto.GalleryItemRequest;
import com.successacademy.galleryservice.model.GalleryItem;
import com.successacademy.galleryservice.service.GalleryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/gallery")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService service;

    // ── PUBLIC endpoints (whitelisted in gateway) ─────────────────

    // Get all public gallery items
    @GetMapping("/public")
    public ResponseEntity<List<GalleryItem>> publicItems() {
        return ResponseEntity.ok(service.getPublicItems());
    }

    // Get public items by category
    @GetMapping("/public/category")
    public ResponseEntity<List<GalleryItem>> publicByCategory(@RequestParam String category) {
        return ResponseEntity.ok(service.getPublicItemsByCategory(category));
    }

    // Serve uploaded image files
    @GetMapping("/uploads/{filename}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads/gallery").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) contentType = "application/octet-stream";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── ADMIN endpoints (require JWT via gateway) ─────────────────

    // Get ALL items (admin view — includes hidden)
    @GetMapping("/all")
    public ResponseEntity<List<GalleryItem>> allItems() {
        return ResponseEntity.ok(service.getAllItems());
    }

    // Get single item
    @GetMapping("/{id}")
    public ResponseEntity<GalleryItem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    // Add item via URL
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public GalleryItem addItem(@RequestBody GalleryItemRequest request) {
        return service.addItem(request);
    }

    // Upload image file + create gallery item
    @PostMapping("/upload")
    public ResponseEntity<GalleryItem> uploadImage(
            @RequestParam("file")        MultipartFile file,
            @RequestParam("title")       String title,
            @RequestParam(value = "description",   required = false, defaultValue = "") String description,
            @RequestParam(value = "category",      required = false, defaultValue = "Other") String category,
            @RequestParam(value = "publicVisible", required = false, defaultValue = "true") boolean publicVisible,
            @RequestParam(value = "createdBy",     required = false, defaultValue = "admin") String createdBy
    ) {
        try {
            GalleryItem item = service.uploadImage(file, title, description, category, publicVisible, createdBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update item
    @PutMapping("/{id}")
    public ResponseEntity<GalleryItem> updateItem(@PathVariable Long id,
                                                   @RequestBody GalleryItemRequest request) {
        return ResponseEntity.ok(service.updateItem(id, request));
    }

    // Delete item (also removes uploaded file from disk)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
    }

    // Toggle active/inactive
    @PutMapping("/{id}/active")
    public ResponseEntity<GalleryItem> toggleActive(@PathVariable Long id,
                                                     @RequestParam boolean status) {
        return ResponseEntity.ok(service.toggleActive(id, status));
    }

    // Toggle public/private visibility
    @PutMapping("/{id}/public")
    public ResponseEntity<GalleryItem> togglePublic(@PathVariable Long id,
                                                     @RequestParam boolean status) {
        return ResponseEntity.ok(service.togglePublic(id, status));
    }

    // Search by title
    @GetMapping("/search")
    public ResponseEntity<List<GalleryItem>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByTitle(keyword));
    }
}
