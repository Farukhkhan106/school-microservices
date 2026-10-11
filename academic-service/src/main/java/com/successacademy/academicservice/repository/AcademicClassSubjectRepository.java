package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicClassSubjectRepository extends JpaRepository<AcademicClassSubject, Long> {

    List<AcademicClassSubject> findByTenantIdAndSessionId(String tenantId, Long sessionId);

    List<AcademicClassSubject> findByTenantIdAndSessionIdAndStatus(String tenantId, Long sessionId, String status);

    List<AcademicClassSubject> findByTenantIdAndSessionIdAndClassCode(String tenantId, Long sessionId, String classCode);

    Optional<AcademicClassSubject> findByTenantIdAndSessionIdAndClassCodeAndSubjectCode(String tenantId, Long sessionId, String classCode, String subjectCode);

    boolean existsByTenantIdAndSessionIdAndClassCodeAndSubjectCode(String tenantId, Long sessionId, String classCode, String subjectCode);
}
