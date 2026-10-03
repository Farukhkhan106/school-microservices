package com.successacademy.staffservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.service.url:http://localhost:8084}")
    private String authServiceUrl;

    /**
     * Provisions a login user for a staff member.
     * Role is always STAFF.
     */
    public Long createStaffUser(Long staffId, String username, String password, String email) {
        try {
            String finalUsername = ensureUniqueUsername(username);

            Map<String, Object> body = Map.of(
                "username",  finalUsername,
                "password",  password,
                "email",     (email != null && !email.isBlank()) ? email : finalUsername + "@staff.successacademy.edu.in",
                "role",      "STAFF",
                "staffId",   staffId,
                "studentId", 0,
                "teacherId", 0
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                authServiceUrl + "/auth/register", entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Object id = response.getBody().get("id");
                log.info("✅ Staff login provisioned: username={} staffId={} userId={}", finalUsername, staffId, id);
                return id instanceof Number ? ((Number) id).longValue() : null;
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not provision auth user for staff {}: {}", username, e.getMessage());
        }
        return null;
    }

    public boolean usernameExists(String username) {
        try {
            ResponseEntity<Boolean> resp = restTemplate.getForEntity(
                authServiceUrl + "/auth/exists/" + username.toLowerCase().trim(), Boolean.class
            );
            return Boolean.TRUE.equals(resp.getBody());
        } catch (Exception ignored) {
            return false;
        }
    }

    public String ensureUniqueUsername(String base) {
        String sanitized = base.toLowerCase().trim().replaceAll("[^a-z0-9.]", "").replaceAll("\\.+", ".");
        if (sanitized.isBlank()) sanitized = "staff";

        try {
            if (usernameExists(sanitized)) {
                for (int i = 2; i <= 999; i++) {
                    String candidate = sanitized + i;
                    if (!usernameExists(candidate)) {
                        return candidate;
                    }
                }
            }
        } catch (Exception ignored) {}
        return sanitized;
    }
}
