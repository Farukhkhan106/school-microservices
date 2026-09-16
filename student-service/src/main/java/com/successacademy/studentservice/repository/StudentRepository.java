package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByStudentClassAndSectionIgnoreCase(String studentClass, String section);
    List<Student> findByStudentClass(String studentClass);
    boolean existsByAdmissionNoIgnoreCase(String admissionNo);
    boolean existsByEmailIgnoreCase(String email);
}
