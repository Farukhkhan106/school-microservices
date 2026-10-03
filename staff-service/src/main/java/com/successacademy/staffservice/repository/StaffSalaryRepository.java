package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffSalaryRepository extends JpaRepository<StaffSalary, Long> {

    Optional<StaffSalary> findByStaffId(Long staffId);

    Optional<StaffSalary> findByStaffIdAndActiveTrue(Long staffId);

    boolean existsByStaffId(Long staffId);
}
