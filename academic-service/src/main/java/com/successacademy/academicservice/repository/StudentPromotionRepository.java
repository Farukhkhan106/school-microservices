package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.StudentPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentPromotionRepository extends JpaRepository<StudentPromotion, Long> {

    List<StudentPromotion> findByTenantIdAndTransitionId(String tenantId, Long transitionId);

    Optional<StudentPromotion> findByTenantIdAndTransitionIdAndStudentId(String tenantId, Long transitionId, Long studentId);

    void deleteByTenantIdAndTransitionId(String tenantId, Long transitionId);
}
