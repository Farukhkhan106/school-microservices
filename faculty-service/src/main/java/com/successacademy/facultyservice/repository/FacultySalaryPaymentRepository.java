package com.successacademy.facultyservice.repository;

import com.successacademy.facultyservice.model.FacultySalaryPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultySalaryPaymentRepository extends JpaRepository<FacultySalaryPayment, Long> {

    List<FacultySalaryPayment> findByMonthlySalaryIdOrderByPaymentDateAsc(Long monthlySalaryId);

    List<FacultySalaryPayment> findByFacultyIdOrderByPaymentDateDesc(Long facultyId);

    Optional<FacultySalaryPayment> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);
}
