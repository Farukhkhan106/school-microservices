package com.successacademy.staffservice.service;

import com.successacademy.staffservice.dto.StaffAuditLogResponse;
import com.successacademy.staffservice.model.StaffAuditLog;
import com.successacademy.staffservice.repository.StaffAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final StaffAuditLogRepository repository;

    @Override
    public void log(Long actorUserId, Long staffId, String action, String oldValue, String newValue, String reason) {
        try {
            StaffAuditLog entry = StaffAuditLog.builder()
                    .actorUserId(actorUserId)
                    .staffId(staffId)
                    .action(action)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .reason(reason)
                    .timestamp(LocalDateTime.now())
                    .build();
            repository.save(entry);
        } catch (Exception e) {
            log.warn("⚠️ Failed to write audit log: {}", e.getMessage());
        }
    }

    @Override
    public List<StaffAuditLogResponse> getLogsForStaff(Long staffId) {
        return repository.findByStaffIdOrderByTimestampDesc(staffId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private StaffAuditLogResponse mapToResponse(StaffAuditLog l) {
        return StaffAuditLogResponse.builder()
                .id(l.getId())
                .actorUserId(l.getActorUserId())
                .staffId(l.getStaffId())
                .action(l.getAction())
                .oldValue(l.getOldValue())
                .newValue(l.getNewValue())
                .reason(l.getReason())
                .timestamp(l.getTimestamp())
                .build();
    }
}
