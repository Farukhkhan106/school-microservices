package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.Assessment;
import com.successacademy.academicservice.model.AssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findBySessionIdOrderByStartDateDesc(Long sessionId);
    List<Assessment> findBySessionIdAndStatus(Long sessionId, AssessmentStatus status);
}
