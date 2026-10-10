package com.successacademy.academicservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class CommunicationServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${communication.service.url:http://localhost:8091}")
    private String communicationServiceUrl;

    @Value("${app.gateway.internal-secret:${INTERNAL_GATEWAY_SECRET:success-academy-secure-internal-gateway-token-2026}}")
    private String internalGatewaySecret;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateNotificationDto {
        private Long userId;
        private String username;
        private String userRole;
        private String title;
        private String message;
        private String type;
        private String entityType;
        private String entityId;
        private String actionUrl;
    }

    public void sendNotification(CreateNotificationDto dto, String tenantId) {
        if (dto == null || dto.getUserId() == null) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", internalGatewaySecret);
            headers.set("X-User-Role", "ADMIN");
            headers.set("X-Tenant-Id", tenantId != null ? tenantId : "default");

            HttpEntity<CreateNotificationDto> entity = new HttpEntity<>(dto, headers);
            String url = communicationServiceUrl + "/communication/notifications";
            restTemplate.postForObject(url, entity, Object.class);
        } catch (Exception e) {
            log.debug("Non-fatal: could not send notification to user {}: {}", dto.getUserId(), e.getMessage());
        }
    }
}
