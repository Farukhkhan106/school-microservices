package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByStudentClassAndSection(String studentClass, String section);
    List<ClassSchedule> findByTeacherId(Long teacherId);
    List<ClassSchedule> findByTeacherIdAndDayOfWeek(Long teacherId, String dayOfWeek);
    List<ClassSchedule> findByStudentClass(String studentClass);
}
