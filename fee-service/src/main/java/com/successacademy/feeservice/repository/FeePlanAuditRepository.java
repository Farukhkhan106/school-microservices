package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.FeePlanAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeePlanAuditRepository extends JpaRepository<FeePlanAudit, Long> {

    List<FeePlanAudit> findByFeePlanIdOrderByChangedAtDesc(Long feePlanId);
}
