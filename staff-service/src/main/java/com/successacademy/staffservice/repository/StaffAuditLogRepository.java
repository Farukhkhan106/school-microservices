package com.successacademy.staffservice.repository;

import com.successacademy.staffservice.model.StaffAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaffAuditLogRepository extends JpaRepository<StaffAuditLog, Long> {

    List<StaffAuditLog> findByStaffIdOrderByTimestampDesc(Long staffId);
}
