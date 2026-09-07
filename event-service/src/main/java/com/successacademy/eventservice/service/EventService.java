package com.successacademy.eventservice.service;

import com.successacademy.eventservice.model.Event;

import java.time.LocalDate;
import java.util.List;

public interface EventService {

    Event addEvent(Event event);

    Event updateEvent(Long id, Event event);

    void deleteEvent(Long id);

    List<Event> getAllEvents();

    List<Event> getActiveEvents();

    // Public website — active=true, ordered by date
    List<Event> getPublicEvents();

    // Upcoming — eventDate >= today, active=true
    List<Event> getUpcomingEvents();

    // Past — eventDate < today, active=true
    List<Event> getPastEvents();

    Event getEventById(Long id);

    List<Event> searchByTitle(String keyword);

    List<Event> filterByDate(LocalDate date);

    Event toggleActive(Long id, boolean active);
}
