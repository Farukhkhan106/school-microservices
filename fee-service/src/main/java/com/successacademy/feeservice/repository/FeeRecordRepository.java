package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findByStudentId(Long studentId);

    List<FeeRecord> findByStatus(String status);

    List<FeeRecord> findByStudentNameContainingIgnoreCase(String name);

    // Sum of all paidAmount (total revenue)
    @Query("SELECT COALESCE(SUM(f.paidAmount), 0) FROM FeeRecord f")
    BigDecimal sumAllPaidAmount();

    // Sum of pending (amount - paidAmount) where not fully paid
    @Query("SELECT COALESCE(SUM(f.amount - f.paidAmount), 0) FROM FeeRecord f WHERE f.status != 'Paid'")
    BigDecimal sumPendingAmount();
}
