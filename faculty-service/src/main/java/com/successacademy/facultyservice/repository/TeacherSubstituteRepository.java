package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.TeacherSubstitute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeacherSubstituteRepository extends JpaRepository<TeacherSubstitute, Long> {
    List<TeacherSubstitute> findByDate(LocalDate date);
    List<TeacherSubstitute> findByAbsentTeacherId(Long absentTeacherId);
    List<TeacherSubstitute> findBySubstituteTeacherId(Long substituteTeacherId);
    List<TeacherSubstitute> findBySubstituteTeacherIdAndDate(Long substituteTeacherId, LocalDate date);
}
