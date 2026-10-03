package com.successacademy.academicservice.repository;

import com.successacademy.academicservice.model.AcademicAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicAuditLogRepository extends JpaRepository<AcademicAuditLog, Long> {
    List<AcademicAuditLog> findByTargetEntityAndTargetIdOrderByTimestampDesc(String targetEntity, Long targetId);
    List<AcademicAuditLog> findTop50ByOrderByTimestampDesc();
}
