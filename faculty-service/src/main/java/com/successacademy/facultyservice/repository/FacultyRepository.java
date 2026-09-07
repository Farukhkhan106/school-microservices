package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    List<Faculty> findByStatusIgnoreCase(String status);

    List<Faculty> findByNameContainingIgnoreCase(String keyword);

    List<Faculty> findBySubjectsContainingIgnoreCase(String subject);

    Optional<Faculty> findByUserId(Long userId);

    Optional<Faculty> findByClassTeacherOf(String className);
}
