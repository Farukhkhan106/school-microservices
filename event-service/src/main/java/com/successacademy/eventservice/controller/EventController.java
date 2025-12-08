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
@CrossOrigin("*")
public class EventController {

    private final EventService service;

    // Add Event
    @PostMapping("/add")
    public ResponseEntity<Event> add(@RequestBody Event event) {
        return ResponseEntity.ok(service.addEvent(event));
    }

    // Update Event
    @PutMapping("/update/{id}")
    public ResponseEntity<Event> update(@PathVariable Long id, @RequestBody Event event) {
        return ResponseEntity.ok(service.updateEvent(id, event));
    }

    // Delete Event
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteEvent(id);
        return ResponseEntity.ok("Event deleted");
    }

    // Get All Events
    @GetMapping("/all")
    public ResponseEntity<List<Event>> all() {
        return ResponseEntity.ok(service.getAllEvents());
    }

    // Get Active Events
    @GetMapping("/active")
    public ResponseEntity<List<Event>> activeEvents() {
        return ResponseEntity.ok(service.getActiveEvents());
    }

    // Get Event by ID
    @GetMapping("/{id}")
    public ResponseEntity<Event> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEventById(id));
    }

    // Search by Title
    @GetMapping("/search")
    public ResponseEntity<List<Event>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchByTitle(keyword));
    }

    // Filter by Date
    @GetMapping("/filter")
    public ResponseEntity<List<Event>> filter(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.filterByDate(date));
    }

    // Toggle Active/Inactive
    @PutMapping("/active/{id}")
    public ResponseEntity<Event> toggle(@PathVariable Long id, @RequestParam boolean status) {
        return ResponseEntity.ok(service.toggleActive(id, status));
    }
}
