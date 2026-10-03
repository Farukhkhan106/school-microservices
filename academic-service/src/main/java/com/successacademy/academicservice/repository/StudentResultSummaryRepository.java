package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.StudentResultSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentResultSummaryRepository extends JpaRepository<StudentResultSummary, Long> {
    Optional<StudentResultSummary> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);
    List<StudentResultSummary> findByAssessmentIdAndStudentClassAndSectionOrderByPercentageDesc(
        Long assessmentId, String studentClass, String section
    );
    List<StudentResultSummary> findByAssessmentIdAndStudentClassOrderByPercentageDesc(
        Long assessmentId, String studentClass
    );
    List<StudentResultSummary> findByStudentIdOrderByCalculatedAtDesc(Long studentId);
}
