package com.successacademy.eventservice.service;

import com.successacademy.eventservice.model.Event;
import com.successacademy.eventservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository repository;

    @Override
    public Event addEvent(Event event) {
        // Auto-set type based on date if not provided
        if (event.getType() == null) {
            event.setType(deriveType(event.getEventDate()));
        }
        return repository.save(event);
    }

    @Override
    public Event updateEvent(Long id, Event event) {
        Event existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        existing.setTitle(event.getTitle());
        existing.setDescription(event.getDescription());
        existing.setEventDate(event.getEventDate());
        existing.setEventTime(event.getEventTime());
        existing.setLocation(event.getLocation());
        existing.setImageUrl(event.getImageUrl());
        existing.setCreatedBy(event.getCreatedBy());
        existing.setActive(event.isActive());
        // re-derive type if date changed
        existing.setType(event.getType() != null
                ? event.getType()
                : deriveType(event.getEventDate()));

        return repository.save(existing);
    }

    @Override
    public void deleteEvent(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<Event> getAllEvents() {
        return repository.findAll()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
    }

    @Override
    public List<Event> getActiveEvents() {
        return repository.findByActiveTrue()
                .stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .toList();
    }

    @Override
    public List<Event> getPublicEvents() {
        return repository.findByActiveTrueOrderByEventDateAsc();
    }

    @Override
    public List<Event> getUpcomingEvents() {
        return repository.findByEventDateGreaterThanEqualAndActiveTrue(LocalDate.now());
    }

    @Override
    public List<Event> getPastEvents() {
        return repository.findByEventDateLessThanAndActiveTrue(LocalDate.now());
    }

    @Override
    public Event getEventById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }

    @Override
    public List<Event> searchByTitle(String keyword) {
        return repository.findByTitleContainingIgnoreCase(keyword);
    }

    @Override
    public List<Event> filterByDate(LocalDate date) {
        return repository.findByEventDate(date);
    }

    @Override
    public Event toggleActive(Long id, boolean active) {
        Event event = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        event.setActive(active);
        return repository.save(event);
    }

    // ── HELPER ──────────────────────────────────────────────────

    private String deriveType(LocalDate date) {
        if (date == null) return "Upcoming";
        return date.isBefore(LocalDate.now()) ? "Past" : "Upcoming";
    }
}
