package com.successacademy.studentservice.repository;

import com.successacademy.studentservice.model.StudentLifecycleLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentLifecycleLogRepository extends JpaRepository<StudentLifecycleLog, Long> {

    List<StudentLifecycleLog> findByTenantIdAndStudentIdOrderByCreatedAtDesc(String tenantId, Long studentId);

    default List<StudentLifecycleLog> findByStudentIdAndTenantIdOrderByPerformedAtDesc(Long studentId, String tenantId) {
        return findByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId);
    }
}
