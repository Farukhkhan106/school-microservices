package com.successacademy.facultyservice.controller;

import com.successacademy.facultyservice.dto.FacultyRequest;
import com.successacademy.facultyservice.dto.FacultyResponse;
import com.successacademy.facultyservice.service.FacultyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faculty")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyService service;

    // ── ADMIN CRUD ──────────────────────────────────────────────

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public FacultyResponse add(@RequestBody FacultyRequest request) {
        return service.addFaculty(request);
    }

    @PutMapping("/{id}")
    public FacultyResponse update(@PathVariable Long id,
                                  @RequestBody FacultyRequest request) {
        return service.updateFaculty(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteFaculty(id);
    }

    // ── ADMIN / INTERNAL READ ───────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<List<FacultyResponse>> all() {
        return ResponseEntity.ok(service.getAllFaculty());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getFacultyById(id));
    }

    // Get faculty by linked auth userId (for teacher portal)
    @GetMapping("/user/{userId}")
    public ResponseEntity<FacultyResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getFacultyByUserId(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FacultyResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByName(keyword));
    }

    @GetMapping("/subject")
    public ResponseEntity<List<FacultyResponse>> bySubject(@RequestParam String subject) {
        return ResponseEntity.ok(service.filterBySubject(subject));
    }

    // ── PUBLIC (NO AUTH — whitelisted in gateway) ───────────────

    // Active faculty for public website faculty page
    @GetMapping("/public")
    public ResponseEntity<List<FacultyResponse>> publicFaculty() {
        return ResponseEntity.ok(service.getActiveFaculty());
    }

    @PostMapping("/{id}/photo")
    public java.util.Map<String, String> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = service.uploadPhoto(id, file);
        return java.util.Map.of("photoUrl", url);
    }

    @PostMapping("/upload-photo")
    public java.util.Map<String, String> uploadGeneralPhoto(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String url = service.uploadGeneralPhoto(file);
        return java.util.Map.of("photoUrl", url);
    }
}
