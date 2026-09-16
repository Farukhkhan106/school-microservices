package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.StudentFeePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFeePlanRepository extends JpaRepository<StudentFeePlan, Long> {

    Optional<StudentFeePlan> findByStudentIdAndAcademicYear(Long studentId, String academicYear);

    List<StudentFeePlan> findByStudentId(Long studentId);

    List<StudentFeePlan> findByAcademicYear(String academicYear);

    List<StudentFeePlan> findByAcademicYearAndStudentClass(String academicYear, String studentClass);

    @Query("SELECT p FROM StudentFeePlan p WHERE p.studentId = :studentId ORDER BY p.createdAt DESC")
    List<StudentFeePlan> findLatestByStudentId(@Param("studentId") Long studentId);

    boolean existsByStudentIdAndAcademicYear(Long studentId, String academicYear);
}
