package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicSession;
import com.successacademy.academicservice.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicSessionRepository extends JpaRepository<AcademicSession, Long> {
    
    // Backward compatible queries
    Optional<AcademicSession> findByIsActiveTrue();
    Optional<AcademicSession> findBySessionCode(String sessionCode);

    // Tenant-aware queries
    List<AcademicSession> findByTenantIdOrderByStartDateDesc(String tenantId);
    Optional<AcademicSession> findByTenantIdAndIsActiveTrue(String tenantId);
    Optional<AcademicSession> findByTenantIdAndStatus(String tenantId, SessionStatus status);
    Optional<AcademicSession> findByTenantIdAndSessionCode(String tenantId, String sessionCode);
    boolean existsByTenantIdAndSessionCode(String tenantId, String sessionCode);
}
