package com.successacademy.academicservice.service;

import com.successacademy.academicservice.model.AcademicAuditLog;
import com.successacademy.academicservice.repository.AcademicAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AcademicAuditLogRepository auditLogRepository;

    public void log(Long actorUserId, String actorRole, String action, String targetEntity, Long targetId, String oldValue, String newValue, String reason) {
        AcademicAuditLog log = AcademicAuditLog.builder()
                .actorUserId(actorUserId)
                .actorRole(actorRole)
                .action(action)
                .targetEntity(targetEntity)
                .targetId(targetId)
                .oldValue(oldValue)
                .newValue(newValue)
                .reason(reason)
                .build();
        auditLogRepository.save(log);
    }
}
