package com.successacademy.authservice.service.user;

import com.successacademy.authservice.dto.user.UserAuditLogDto;
import com.successacademy.authservice.model.entity.UserAuditLog;
import com.successacademy.authservice.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuditService {

    private final UserAuditLogRepository auditLogRepository;

    @Transactional
    public void log(String tenantId, Long actorUserId, String actorUsername, String actorRole,
                    Long targetUserId, String targetUsername, String action, String details) {
        try {
            UserAuditLog logEntry = UserAuditLog.builder()
                    .tenantId((tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default")
                    .actorUserId(actorUserId)
                    .actorUsername(actorUsername != null ? actorUsername : "SYSTEM")
                    .actorRole(actorRole != null ? actorRole : "UNKNOWN")
                    .targetUserId(targetUserId != null ? targetUserId : 0L)
                    .targetUsername(targetUsername != null ? targetUsername : "UNKNOWN")
                    .action(action)
                    .details(details)
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception ex) {
            log.error("Failed to write user audit log for action {}: {}", action, ex.getMessage());
        }
    }

    public List<UserAuditLogDto> getAuditLogsForUser(Long targetUserId) {
        return auditLogRepository.findByTargetUserIdOrderByTimestampDesc(targetUserId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<UserAuditLogDto> getAuditLogsForTenant(String tenantId, Pageable pageable) {
        String cleanTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId.trim() : "default";
        return auditLogRepository.findByTenantIdOrderByTimestampDesc(cleanTenant, pageable)
                .map(this::toDto);
    }

    private UserAuditLogDto toDto(UserAuditLog log) {
        return UserAuditLogDto.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .actorUserId(log.getActorUserId())
                .actorUsername(log.getActorUsername())
                .actorRole(log.getActorRole())
                .targetUserId(log.getTargetUserId())
                .targetUsername(log.getTargetUsername())
                .action(log.getAction())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }
}
