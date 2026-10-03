package com.successacademy.eventservice.controller;

import com.successacademy.eventservice.model.Event;
import com.successacademy.eventservice.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

    @Value("${event.upload-dir:uploads/events}")
    private String uploadDir;

    // ── PUBLIC FILE SERVING ─────────────────────────────────────

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── IMAGE UPLOAD ─────────────────────────────────────────────

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
        }

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/event/uploads/" + uniqueFilename;
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("imageUrl", imageUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to store image: " + e.getMessage()));
        }
    }

    // ── ADMIN CRUD ──────────────────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<Event> add(@RequestBody Event event) {
        return ResponseEntity.ok(service.addEvent(event));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Event> update(@PathVariable Long id, @RequestBody Event event) {
        return ResponseEntity.ok(service.updateEvent(id, event));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteEvent(id);
        return ResponseEntity.ok("Event deleted");
    }

    @PutMapping("/active/{id}")
    public ResponseEntity<Event> toggle(
            @PathVariable Long id,
            @RequestParam boolean status) {
        return ResponseEntity.ok(service.toggleActive(id, status));
    }

    // ── ADMIN / INTERNAL READ ───────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<Event>> all() {
        return ResponseEntity.ok(service.getAllEvents());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Event>> activeEvents() {
        return ResponseEntity.ok(service.getActiveEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEventById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Event>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByTitle(keyword));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Event>> filter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.filterByDate(date));
    }

    // ── PUBLIC (NO AUTH — whitelisted in gateway) ───────────────

    // All active events for public website
    @GetMapping("/public")
    public ResponseEntity<List<Event>> publicEvents() {
        return ResponseEntity.ok(service.getPublicEvents());
    }

    // Upcoming events only (eventDate >= today)
    @GetMapping("/public/upcoming")
    public ResponseEntity<List<Event>> upcoming() {
        return ResponseEntity.ok(service.getUpcomingEvents());
    }

    // Past events only (eventDate < today)
    @GetMapping("/public/past")
    public ResponseEntity<List<Event>> past() {
        return ResponseEntity.ok(service.getPastEvents());
    }
}
