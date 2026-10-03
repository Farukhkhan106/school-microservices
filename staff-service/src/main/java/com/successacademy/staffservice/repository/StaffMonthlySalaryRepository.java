package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffMonthlySalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffMonthlySalaryRepository extends JpaRepository<StaffMonthlySalary, Long> {

    List<StaffMonthlySalary> findByMonthAndYearOrderByStaffIdAsc(Integer month, Integer year);

    Optional<StaffMonthlySalary> findByStaffIdAndMonthAndYear(Long staffId, Integer month, Integer year);

    List<StaffMonthlySalary> findByStaffIdOrderByYearDescMonthDesc(Long staffId);
}
