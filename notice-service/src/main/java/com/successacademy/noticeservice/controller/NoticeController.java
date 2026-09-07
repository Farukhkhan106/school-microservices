package com.successacademy.noticeservice.controller;

import com.successacademy.noticeservice.model.Notice;
import com.successacademy.noticeservice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService service;

    // ── ADMIN CRUD ──────────────────────────────────────────────

    @PostMapping("/add")
    public ResponseEntity<Notice> add(@RequestBody Notice notice) {
        return ResponseEntity.ok(service.addNotice(notice));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Notice> update(@PathVariable Long id, @RequestBody Notice notice) {
        return ResponseEntity.ok(service.updateNotice(id, notice));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteNotice(id);
        return ResponseEntity.ok("Notice deleted");
    }

    // Toggle active/inactive
    @PutMapping("/active/{id}")
    public ResponseEntity<Notice> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean status) {
        return ResponseEntity.ok(service.toggleActive(id, status));
    }

    // ── ADMIN / INTERNAL READ ───────────────────────────────────

    // All notices (admin view — includes drafts + private)
    @GetMapping("/all")
    public ResponseEntity<List<Notice>> all() {
        return ResponseEntity.ok(service.getAllNotices());
    }

    // Active notices (admin active filter)
    @GetMapping("/active")
    public ResponseEntity<List<Notice>> activeNotices() {
        return ResponseEntity.ok(service.getActiveNotices());
    }

    // Filter by category (admin)
    @GetMapping("/category")
    public ResponseEntity<List<Notice>> byCategory(@RequestParam String category) {
        return ResponseEntity.ok(service.getNoticesByCategory(category));
    }

    // Search by title
    @GetMapping("/search")
    public ResponseEntity<List<Notice>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByTitle(keyword));
    }

    // Filter by date range
    @GetMapping("/filter")
    public ResponseEntity<List<Notice>> filter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.filterByDateRange(start, end));
    }

    // Get single notice by id
    @GetMapping("/{id}")
    public ResponseEntity<Notice> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getNoticeById(id));
    }

    // ── PUBLIC (NO AUTH — whitelisted in gateway) ───────────────

    // Public notices for website homepage & notices page
    @GetMapping("/public")
    public ResponseEntity<List<Notice>> publicNotices() {
        return ResponseEntity.ok(service.getPublicNotices());
    }

    // Public notices filtered by category
    @GetMapping("/public/category")
    public ResponseEntity<List<Notice>> publicByCategory(@RequestParam String category) {
        return ResponseEntity.ok(service.getPublicNoticesByCategory(category));
    }
}
