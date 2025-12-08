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

    Event getEventById(Long id);

    List<Event> searchByTitle(String keyword);

    List<Event> filterByDate(LocalDate date);

    Event toggleActive(Long id, boolean active);
}
