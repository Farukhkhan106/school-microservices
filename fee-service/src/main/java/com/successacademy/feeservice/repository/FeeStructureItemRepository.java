package com.successacademy.feeservice.repository;

import com.successacademy.feeservice.model.FeeStructureItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeStructureItemRepository extends JpaRepository<FeeStructureItem, Long> {

    List<FeeStructureItem> findByFeeStructureIdAndActiveTrue(Long feeStructureId);

    List<FeeStructureItem> findByFeeStructureId(Long feeStructureId);

    void deleteByFeeStructureId(Long feeStructureId);
}
