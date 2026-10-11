package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicSectionRepository extends JpaRepository<AcademicSection, Long> {

    List<AcademicSection> findByTenantIdOrderByDisplayOrderAsc(String tenantId);

    List<AcademicSection> findByTenantIdAndStatusOrderByDisplayOrderAsc(String tenantId, String status);

    Optional<AcademicSection> findByTenantIdAndSectionCode(String tenantId, String sectionCode);

    boolean existsByTenantIdAndSectionCode(String tenantId, String sectionCode);

    boolean existsByTenantIdAndSectionCodeAndIdNot(String tenantId, String sectionCode, Long id);
}
