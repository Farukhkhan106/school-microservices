package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.StaffAuditLogResponse;
import com.successacademy.staffservice.model.StaffAuditLog;
import com.successacademy.staffservice.repository.StaffAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
    void log(Long actorUserId, Long staffId, String action, String oldValue, String newValue, String reason);
    List<StaffAuditLogResponse> getLogsForStaff(Long staffId);
}
