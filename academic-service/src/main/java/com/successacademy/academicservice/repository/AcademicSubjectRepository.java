package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicSubjectRepository extends JpaRepository<AcademicSubject, Long> {

    List<AcademicSubject> findByTenantIdOrderByDisplayOrderAsc(String tenantId);

    List<AcademicSubject> findByTenantIdAndStatusOrderByDisplayOrderAsc(String tenantId, String status);

    Optional<AcademicSubject> findByTenantIdAndSubjectCode(String tenantId, String subjectCode);

    boolean existsByTenantIdAndSubjectCode(String tenantId, String subjectCode);

    boolean existsByTenantIdAndSubjectCodeAndIdNot(String tenantId, String subjectCode, Long id);
}
