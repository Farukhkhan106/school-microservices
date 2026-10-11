package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    Page<UserAuditLog> findByTenantIdOrderByTimestampDesc(String tenantId, Pageable pageable);

    List<UserAuditLog> findByTargetUserIdOrderByTimestampDesc(Long targetUserId);

    List<UserAuditLog> findTop50ByTenantIdOrderByTimestampDesc(String tenantId);
}
