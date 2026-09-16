package com.successacademy.authservice.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;   // BCrypt encoded
    private String role;       // ADMIN / STUDENT / TEACHER

    private String email;

    // Links to other service entities (nullable)
    private Long studentId;    // FK → student-service students.id
    private Long teacherId;    // FK → faculty-service faculty.id

    private String status = "ACTIVE"; // ACTIVE / INACTIVE
}
