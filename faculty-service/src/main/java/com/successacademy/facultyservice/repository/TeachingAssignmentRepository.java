package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Long> {
    List<TeachingAssignment> findByTeacherId(Long teacherId);
    List<TeachingAssignment> findByStudentClassAndSection(String studentClass, String section);
    List<TeachingAssignment> findByStudentClass(String studentClass);
}
