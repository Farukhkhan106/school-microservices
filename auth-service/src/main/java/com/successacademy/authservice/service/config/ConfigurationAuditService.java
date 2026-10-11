package com.successacademy.authservice.service.config;

import com.successacademy.authservice.model.entity.ConfigurationAuditLog;
import com.successacademy.authservice.repository.ConfigurationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationAuditService {

    private final ConfigurationAuditLogRepository repository;

    public void log(String tenantKey, String environmentKey, String actorUsername, String actorRole, String action, String details) {
        try {
            ConfigurationAuditLog entry = ConfigurationAuditLog.builder()
                    .tenantKey(tenantKey != null ? tenantKey : "default")
                    .environmentKey(environmentKey)
                    .actorUsername(actorUsername != null ? actorUsername : "system")
                    .actorRole(actorRole != null ? actorRole : "SYSTEM")
                    .action(action)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write configuration audit log: {}", e.getMessage());
        }
    }

    public List<ConfigurationAuditLog> getLogsForTenant(String tenantKey) {
        return repository.findByTenantKeyOrderByTimestampDesc(tenantKey);
    }
}
