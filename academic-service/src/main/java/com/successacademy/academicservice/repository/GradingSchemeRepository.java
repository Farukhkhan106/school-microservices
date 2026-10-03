package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.GradingScheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradingSchemeRepository extends JpaRepository<GradingScheme, Long> {
    Optional<GradingScheme> findByIsDefaultTrue();
}
