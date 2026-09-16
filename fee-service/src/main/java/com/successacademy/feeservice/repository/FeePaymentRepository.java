package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    List<FeePayment> findByFeeRecordIdOrderByCreatedAtDesc(Long feeRecordId);

    long countByFeeRecordId(Long feeRecordId);

    Optional<FeePayment> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    boolean existsByTransactionReferenceAndFeeRecordId(String transactionReference, Long feeRecordId);

    @Query("SELECT COUNT(p) FROM FeePayment p WHERE p.receiptNumber LIKE CONCAT('REC-', :year, '-%')")
    long countReceiptsByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM FeePayment p WHERE p.feeRecordId = :feeRecordId")
    BigDecimal sumPaidByFeeRecordId(@Param("feeRecordId") Long feeRecordId);
}
