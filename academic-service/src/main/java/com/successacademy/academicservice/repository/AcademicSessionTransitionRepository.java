package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicSessionTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicSessionTransitionRepository extends JpaRepository<AcademicSessionTransition, Long> {

    List<AcademicSessionTransition> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<AcademicSessionTransition> findByTenantIdAndSourceSessionIdAndTargetSessionId(
            String tenantId, Long sourceSessionId, Long targetSessionId
    );
}
