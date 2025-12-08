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
        return repository.save(event);
    }

    @Override
    public Event updateEvent(Long id, Event event) {
        Event existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        existing.setTitle(event.getTitle());
        existing.setDescription(event.getDescription());
        existing.setEventDate(event.getEventDate());
        existing.setLocation(event.getLocation());
        existing.setActive(event.isActive());

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
    public Event getEventById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found"));
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
                .orElseThrow(() -> new RuntimeException("Event not found"));

        event.setActive(active);
        return repository.save(event);
    }
}
