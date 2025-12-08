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

    private String username;   // admin username
    private String password;   // encrypted password
    private String role;       // ADMIN
}
