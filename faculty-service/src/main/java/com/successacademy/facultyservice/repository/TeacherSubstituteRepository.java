package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.TeacherSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherSubstituteRepository extends JpaRepository<TeacherSubstitute, Long> {

    List<TeacherSubstitute> findByDateOrderByCreatedAtDesc(LocalDate date);

    List<TeacherSubstitute> findByAbsentTeacherIdAndDate(Long absentTeacherId, LocalDate date);

    List<TeacherSubstitute> findBySubstituteTeacherIdAndDateAndStatus(
            Long substituteTeacherId, LocalDate date, String status);

    List<TeacherSubstitute> findByDateAndStudentClassAndSectionIgnoreCase(
            LocalDate date, String studentClass, String section);

    List<TeacherSubstitute> findByAbsentTeacherIdOrderByDateDesc(Long absentTeacherId);

    List<TeacherSubstitute> findBySubstituteTeacherIdOrderByDateDesc(Long substituteTeacherId);

    // Clash detection: Is substitute teacher already assigned as a substitute on this date and period?
    @Query("SELECT s FROM TeacherSubstitute s WHERE s.substituteTeacherId = :subId " +
           "AND s.date = :date " +
           "AND s.status = 'ASSIGNED' " +
           "AND (s.periodNo = :periodNo OR s.periodNo IS NULL OR :periodNo IS NULL)")
    List<TeacherSubstitute> findClashingSubstitutions(
            @Param("subId") Long subId,
            @Param("date") LocalDate date,
            @Param("periodNo") Integer periodNo);

    // Active substitution for Class Teacher cover on a date
    @Query("SELECT s FROM TeacherSubstitute s WHERE s.substituteTeacherId = :subId " +
           "AND s.studentClass = :studentClass " +
           "AND LOWER(s.section) = LOWER(:section) " +
           "AND s.date = :date " +
           "AND s.classTeacherCover = true " +
           "AND s.status = 'ASSIGNED'")
    List<TeacherSubstitute> findActiveClassTeacherCovers(
            @Param("subId") Long subId,
            @Param("studentClass") String studentClass,
            @Param("section") String section,
            @Param("date") LocalDate date);

    // Any active substitution for a class on a date
    @Query("SELECT s FROM TeacherSubstitute s WHERE s.substituteTeacherId = :subId " +
           "AND s.studentClass = :studentClass " +
           "AND LOWER(s.section) = LOWER(:section) " +
           "AND s.date = :date " +
           "AND s.status = 'ASSIGNED'")
    List<TeacherSubstitute> findActiveCoversForClass(
            @Param("subId") Long subId,
            @Param("studentClass") String studentClass,
            @Param("section") String section,
            @Param("date") LocalDate date);
}
