package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.FacultyMonthlySalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyMonthlySalaryRepository extends JpaRepository<FacultyMonthlySalary, Long> {

    Optional<FacultyMonthlySalary> findByFacultyIdAndMonthAndYear(Long facultyId, Integer month, Integer year);

    List<FacultyMonthlySalary> findByMonthAndYear(Integer month, Integer year);

    List<FacultyMonthlySalary> findByFacultyIdOrderByYearDescMonthDesc(Long facultyId);
}
