package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.FacultyAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyAttendanceRepository extends JpaRepository<FacultyAttendance, Long> {

    Optional<FacultyAttendance> findByFacultyIdAndAttendanceDate(Long facultyId, LocalDate attendanceDate);

    List<FacultyAttendance> findByAttendanceDate(LocalDate attendanceDate);

    List<FacultyAttendance> findByFacultyIdAndAttendanceDateBetween(Long facultyId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT fa FROM FacultyAttendance fa WHERE YEAR(fa.attendanceDate) = :year AND MONTH(fa.attendanceDate) = :month")
    List<FacultyAttendance> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query("SELECT fa FROM FacultyAttendance fa WHERE fa.facultyId = :facultyId AND YEAR(fa.attendanceDate) = :year AND MONTH(fa.attendanceDate) = :month")
    List<FacultyAttendance> findByFacultyIdAndYearAndMonth(@Param("facultyId") Long facultyId, @Param("year") int year, @Param("month") int month);

    List<FacultyAttendance> findByFacultyIdOrderByAttendanceDateDesc(Long facultyId);
}
