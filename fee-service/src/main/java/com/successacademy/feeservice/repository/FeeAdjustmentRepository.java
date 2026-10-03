package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.FeeAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeAdjustmentRepository extends JpaRepository<FeeAdjustment, Long> {

    List<FeeAdjustment> findByFeePlanId(Long feePlanId);

    void deleteByFeePlanId(Long feePlanId);
}
