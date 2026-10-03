package com.successacademy.facultyservice.client;

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
     * Creates login for a teacher/faculty.
     * Username: firstname.lastname
     * Password: EMP + facultyId (e.g. EMP001)
     */
    public Long createTeacherUser(Long facultyId, String name, String email) {
        String[] parts = name.trim().split("\\s+");
        // Skip titles like Dr./Mr./Mrs./Ms.
        int start = 0;
        if (parts[0].matches("(?i)Dr\\.|Mr\\.|Mrs\\.|Ms\\.|Prof\\.")) start = 1;
        String firstName = parts.length > start ? parts[start] : parts[0];
        String lastName  = parts.length > start + 1 ? parts[parts.length - 1] : "teacher";

        String username = generateUsername(firstName, lastName);
        String password = "EMP" + String.format("%03d", facultyId);
        String emailFinal = (email != null && !email.isBlank())
            ? email
            : username + "@successacademy.edu.in";

        return createUser(username, password, emailFinal, "TEACHER", facultyId);
    }

    private Long createUser(String username, String password, String email,
                             String role, Long teacherId) {
        try {
            String finalUsername = ensureUniqueUsername(username);

            Map<String, Object> body = Map.of(
                "username",  finalUsername,
                "password",  password,
                "email",     email,
                "role",      role,
                "studentId", 0,
                "teacherId", teacherId
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                authServiceUrl + "/auth/register", entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Object id = response.getBody().get("id");
                log.info("✅ Teacher user created: username={} teacherId={} userId={}", finalUsername, teacherId, id);
                return id instanceof Number ? ((Number) id).longValue() : null;
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not create auth user for teacher {}: {}", username, e.getMessage());
        }
        return null;
    }

    private String ensureUniqueUsername(String base) {
        try {
            ResponseEntity<Boolean> resp = restTemplate.getForEntity(
                authServiceUrl + "/auth/exists/" + base, Boolean.class
            );
            if (Boolean.TRUE.equals(resp.getBody())) {
                for (int i = 2; i <= 99; i++) {
                    String candidate = base + i;
                    ResponseEntity<Boolean> r2 = restTemplate.getForEntity(
                        authServiceUrl + "/auth/exists/" + candidate, Boolean.class
                    );
                    if (!Boolean.TRUE.equals(r2.getBody())) return candidate;
                }
            }
        } catch (Exception ignored) {}
        return base;
    }

    private String generateUsername(String firstName, String lastName) {
        return (firstName + "." + lastName)
            .toLowerCase()
            .replaceAll("[^a-z0-9.]", "")
            .replaceAll("\\.+", ".");
    }
}
