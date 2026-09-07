package com.successacademy.attendanceservice.repository;

import com.successacademy.attendanceservice.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // All attendance for a student
    List<Attendance> findByStudentId(Long studentId);

    // Attendance for a student between two dates
    List<Attendance> findByStudentIdAndDateBetween(Long studentId,
                                                    LocalDate from,
                                                    LocalDate to);

    // Attendance for a class+section on a specific date (teacher fetch)
    List<Attendance> findByStudentClassAndSectionAndDate(String studentClass,
                                                          String section,
                                                          LocalDate date);

    // Attendance for a class on a date (without section filter)
    List<Attendance> findByStudentClassAndDate(String studentClass, LocalDate date);

    // Single record lookup (to avoid duplicate on save)
    Optional<Attendance> findByStudentIdAndDateAndSubject(Long studentId,
                                                           LocalDate date,
                                                           String subject);

    // Count present days for a student
    long countByStudentIdAndStatus(Long studentId, String status);

    // Total days recorded for a student
    long countByStudentId(Long studentId);

    // Per-subject attendance count for a student
    @Query("SELECT a.subject, COUNT(a) FROM Attendance a " +
           "WHERE a.studentId = :studentId AND a.status = 'PRESENT' " +
           "GROUP BY a.subject")
    List<Object[]> countPresentBySubject(@Param("studentId") Long studentId);
}
