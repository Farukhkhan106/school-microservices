package com.successacademy.authservice.repository;

import com.successacademy.authservice.model.entity.ConfigurationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigurationAuditLogRepository extends JpaRepository<ConfigurationAuditLog, Long> {
    List<ConfigurationAuditLog> findByTenantKeyOrderByTimestampDesc(String tenantKey);
}
