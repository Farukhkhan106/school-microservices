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
@CrossOrigin("*")
public class NoticeController {

    private final NoticeService service;

    // Add Notice
    @PostMapping("/add")
    public ResponseEntity<Notice> add(@RequestBody Notice notice) {
        return ResponseEntity.ok(service.addNotice(notice));
    }

    // Update Notice
    @PutMapping("/update/{id}")
    public ResponseEntity<Notice> update(@PathVariable Long id, @RequestBody Notice notice) {
        return ResponseEntity.ok(service.updateNotice(id, notice));
    }

    // Delete Notice
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteNotice(id);
        return ResponseEntity.ok("Notice deleted");
    }

    // Get All Notices
    @GetMapping("/all")
    public ResponseEntity<List<Notice>> all() {
        return ResponseEntity.ok(service.getAllNotices());
    }

    // Get Active Notices
    @GetMapping("/active")
    public ResponseEntity<List<Notice>> activeNotices() {
        return ResponseEntity.ok(service.getActiveNotices());
    }

    // Get Notice by ID
    @GetMapping("/{id}")
    public ResponseEntity<Notice> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getNoticeById(id));
    }

    // Search by Title
    @GetMapping("/search")
    public ResponseEntity<List<Notice>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByTitle(keyword));
    }

    // Filter by Date Range
    @GetMapping("/filter")
    public ResponseEntity<List<Notice>> filter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(service.filterByDateRange(start, end));
    }

    // Toggle Active/Inactive
    @PutMapping("/active/{id}")
    public ResponseEntity<Notice> toggleActive(@PathVariable Long id, @RequestParam boolean status) {
        return ResponseEntity.ok(service.toggleActive(id, status));
    }
}
