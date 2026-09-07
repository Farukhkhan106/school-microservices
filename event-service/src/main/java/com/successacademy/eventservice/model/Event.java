package com.successacademy.eventservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    private LocalDate eventDate;        // date of event

    private String eventTime;           // NEW — e.g. "09:00 AM"

    private String location;

    // NEW — Upcoming | Past (derived from date, but stored for override)
    private String type;

    // NEW — comma-separated image URLs e.g. "url1,url2"
    @Column(length = 1000)
    private String imageUrl;

    private String createdBy;           // NEW — username of creator

    private boolean active = true;
}
