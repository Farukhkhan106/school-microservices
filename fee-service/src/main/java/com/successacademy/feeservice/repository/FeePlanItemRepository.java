package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.FeePlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeePlanItemRepository extends JpaRepository<FeePlanItem, Long> {

    List<FeePlanItem> findByFeePlanId(Long feePlanId);

    void deleteByFeePlanId(Long feePlanId);
}
