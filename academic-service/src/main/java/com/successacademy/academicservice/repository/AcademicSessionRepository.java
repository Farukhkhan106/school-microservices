package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    Optional<AcademicSession> findByIsActiveTrue();
    Optional<AcademicSession> findBySessionCode(String sessionCode);
}
