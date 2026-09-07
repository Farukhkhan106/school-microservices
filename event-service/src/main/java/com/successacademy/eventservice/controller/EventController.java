package com.successacademy.eventservice.controller;

import com.successacademy.eventservice.model.Event;
import com.successacademy.eventservice.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

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
