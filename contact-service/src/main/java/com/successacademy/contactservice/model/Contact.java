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

    private String parentName;

    private String studentName;

    private String email;

    private String phone;

    @Column(length = 2000)
    private String message;

    private LocalDateTime submittedAt = LocalDateTime.now();

    private boolean resolved = false;
}
