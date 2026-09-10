package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findByTeacherIdOrderByDayOfWeekAscPeriodNoAsc(Long teacherId);

    List<ClassSchedule> findByStudentClassAndSectionIgnoreCaseOrderByPeriodNoAsc(String studentClass, String section);

    List<ClassSchedule> findAllByOrderByDayOfWeekAscPeriodNoAsc();

    boolean existsByDayOfWeekIgnoreCaseAndPeriodNoAndTeacherId(String dayOfWeek, int periodNo, Long teacherId);

    boolean existsByDayOfWeekIgnoreCaseAndPeriodNoAndStudentClassAndSectionIgnoreCase(
            String dayOfWeek, int periodNo, String studentClass, String section);

    List<ClassSchedule> findByDayOfWeekIgnoreCaseOrderByPeriodNoAsc(String dayOfWeek);
}