package com.successacademy.eventservice.repository;

import com.successacademy.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByActiveTrue();

    List<Event> findByTitleContainingIgnoreCase(String keyword);

    List<Event> findByEventDate(LocalDate date);
}
