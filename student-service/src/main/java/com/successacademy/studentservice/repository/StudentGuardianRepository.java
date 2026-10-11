package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {

    List<StudentGuardian> findByTenantIdAndStudentId(String tenantId, Long studentId);

    default List<StudentGuardian> findByStudentIdAndTenantId(Long studentId, String tenantId) {
        return findByTenantIdAndStudentId(tenantId, studentId);
    }

    List<StudentGuardian> findByTenantIdAndGuardianId(String tenantId, Long guardianId);

    default List<StudentGuardian> findByGuardianIdAndTenantId(Long guardianId, String tenantId) {
        return findByTenantIdAndGuardianId(tenantId, guardianId);
    }

    Optional<StudentGuardian> findByTenantIdAndStudentIdAndGuardianId(String tenantId, Long studentId, Long guardianId);

    default Optional<StudentGuardian> findByStudentIdAndGuardianIdAndTenantId(Long studentId, Long guardianId, String tenantId) {
        return findByTenantIdAndStudentIdAndGuardianId(tenantId, studentId, guardianId);
    }

    boolean existsByTenantIdAndStudentIdAndGuardianId(String tenantId, Long studentId, Long guardianId);
}
