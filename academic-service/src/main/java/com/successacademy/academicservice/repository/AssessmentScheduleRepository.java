package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AssessmentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentScheduleRepository extends JpaRepository<AssessmentSchedule, Long> {
    List<AssessmentSchedule> findByAssessmentId(Long assessmentId);
    List<AssessmentSchedule> findByTeacherId(Long teacherId);
    List<AssessmentSchedule> findByAssessmentIdAndTeacherId(Long assessmentId, Long teacherId);
    List<AssessmentSchedule> findByAssessmentIdAndStudentClassAndSection(Long assessmentId, String studentClass, String section);
    List<AssessmentSchedule> findByStudentClassAndSection(String studentClass, String section);
    Optional<AssessmentSchedule> findByAssessmentIdAndStudentClassAndSectionAndSubject(
        Long assessmentId, String studentClass, String section, String subject
    );
}
