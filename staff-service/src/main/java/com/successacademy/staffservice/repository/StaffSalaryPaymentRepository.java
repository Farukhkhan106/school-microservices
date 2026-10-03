package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffSalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffSalaryPaymentRepository extends JpaRepository<StaffSalaryPayment, Long> {

    List<StaffSalaryPayment> findByMonthlySalaryIdOrderByCreatedAtAsc(Long monthlySalaryId);

    List<StaffSalaryPayment> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    long count();
}
