package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.TeacherClassAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherClassAssignmentRepository extends JpaRepository<TeacherClassAssignment, Long> {

    List<TeacherClassAssignment> findByTeacherIdOrderById(Long teacherId);

    boolean existsByTeacherIdAndStudentClassAndSectionAndSubjectIgnoreCase(
            Long teacherId, String studentClass, String section, String subject);

    List<TeacherClassAssignment> findByStudentClassAndSectionIgnoreCase(String studentClass, String section);

    List<TeacherClassAssignment> findByTeacherIdAndStudentClassAndSectionIgnoreCase(
            Long teacherId, String studentClass, String section);
}