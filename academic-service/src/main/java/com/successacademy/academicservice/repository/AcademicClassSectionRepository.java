package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicClassSectionRepository extends JpaRepository<AcademicClassSection, Long> {

    List<AcademicClassSection> findByTenantIdAndSessionId(String tenantId, Long sessionId);

    List<AcademicClassSection> findByTenantIdAndSessionIdAndStatus(String tenantId, Long sessionId, String status);

    List<AcademicClassSection> findByTenantIdAndSessionIdAndClassCode(String tenantId, Long sessionId, String classCode);

    Optional<AcademicClassSection> findByTenantIdAndSessionIdAndClassCodeAndSectionCode(String tenantId, Long sessionId, String classCode, String sectionCode);

    boolean existsByTenantIdAndSessionIdAndClassCodeAndSectionCode(String tenantId, Long sessionId, String classCode, String sectionCode);
}
