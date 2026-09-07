package com.successacademy.contactservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String studentName;
    private String email;
    private String phone;

    @Column(length = 2000)
    private String message;

    // Set default directly on field — no @PrePersist needed
    private LocalDateTime submittedAt = LocalDateTime.now();

    private String status = "Pending";

    @Column(length = 1000)
    private String notes;

    // Type of enquiry: "General" (contact form) or "Event Enquiry" (events page)
    private String enquiryType = "General";

    // Name of the event — only filled when enquiryType = "Event Enquiry"
    private String eventTitle;
}
