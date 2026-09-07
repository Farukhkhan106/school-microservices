package com.successacademy.authservice.config;

import com.successacademy.authservice.model.User;
import com.successacademy.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // ── ADMIN ──────────────────────────────────────────────
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@successacademy.edu.in");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("✅ Admin user seeded.");
        }

        // ── STUDENT ────────────────────────────────────────────
        // studentId=1 links to the first student in student-service DB
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setEmail("student@successacademy.edu.in");
            student.setPassword(passwordEncoder.encode("student123"));
            student.setRole("STUDENT");
            student.setStudentId(1L);   // matches student-service students.id = 1
            userRepository.save(student);
            System.out.println("✅ Student user seeded.");
        }

        // ── TEACHER ────────────────────────────────────────────
        // teacherId=1 links to the first teacher in faculty-service DB
        if (userRepository.findByUsername("teacher").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher");
            teacher.setEmail("teacher@successacademy.edu.in");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole("TEACHER");
            teacher.setTeacherId(1L);  // matches faculty-service faculty.id = 1
            userRepository.save(teacher);
            System.out.println("✅ Teacher user seeded.");
        }
    }
}
