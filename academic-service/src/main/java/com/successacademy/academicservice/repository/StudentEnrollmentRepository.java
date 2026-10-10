package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.EnrollmentStatus;
import com.successacademy.academicservice.model.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    List<StudentEnrollment> findByTenantIdAndSessionId(String tenantId, Long sessionId);

    List<StudentEnrollment> findByTenantIdAndSessionIdAndStudentClass(String tenantId, Long sessionId, String studentClass);

    List<StudentEnrollment> findByTenantIdAndSessionIdAndStudentClassAndSection(String tenantId, Long sessionId, String studentClass, String section);

    Optional<StudentEnrollment> findByTenantIdAndSessionIdAndStudentId(String tenantId, Long sessionId, Long studentId);

    List<StudentEnrollment> findByTenantIdAndStudentIdOrderByCreatedAtDesc(String tenantId, Long studentId);

    long countByTenantIdAndSessionId(String tenantId, Long sessionId);

    long countByTenantIdAndSessionIdAndStatus(String tenantId, Long sessionId, EnrollmentStatus status);
}
