package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentMarkRepository extends JpaRepository<StudentMark, Long> {
    List<StudentMark> findByScheduleIdOrderByRollNoAsc(Long scheduleId);
    Optional<StudentMark> findByScheduleIdAndStudentId(Long scheduleId, Long studentId);
    List<StudentMark> findByStudentId(Long studentId);

    @Query("SELECT m FROM StudentMark m WHERE m.schedule.assessment.id = :assessmentId AND m.studentId = :studentId")
    List<StudentMark> findByAssessmentIdAndStudentId(@Param("assessmentId") Long assessmentId, @Param("studentId") Long studentId);

    @Query("SELECT m FROM StudentMark m WHERE m.schedule.assessment.id = :assessmentId AND m.schedule.studentClass = :studentClass AND m.schedule.section = :section")
    List<StudentMark> findByAssessmentIdAndClassAndSection(
        @Param("assessmentId") Long assessmentId,
        @Param("studentClass") String studentClass,
        @Param("section") String section
    );
}
