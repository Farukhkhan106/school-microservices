package com.successacademy.communicationservice.repository;

import com.successacademy.communicationservice.model.CommunicationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunicationAuditLogRepository extends JpaRepository<CommunicationAuditLog, Long> {
    List<CommunicationAuditLog> findByPerformedByOrderByTimestampDesc(Long performedBy);
    List<CommunicationAuditLog> findTop100ByOrderByTimestampDesc();
}
