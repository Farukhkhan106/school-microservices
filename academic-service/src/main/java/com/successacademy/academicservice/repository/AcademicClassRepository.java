package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {

    List<AcademicClass> findByTenantIdOrderByDisplayOrderAsc(String tenantId);

    List<AcademicClass> findByTenantIdAndStatusOrderByDisplayOrderAsc(String tenantId, String status);

    Optional<AcademicClass> findByTenantIdAndClassCode(String tenantId, String classCode);

    boolean existsByTenantIdAndClassCode(String tenantId, String classCode);

    boolean existsByTenantIdAndClassCodeAndIdNot(String tenantId, String classCode, Long id);
}
