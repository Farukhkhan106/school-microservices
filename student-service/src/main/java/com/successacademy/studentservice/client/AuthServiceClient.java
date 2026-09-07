package com.successacademy.studentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Calls auth-service to create a login account when a new student is added.
 * Uses direct service URL (not through gateway) to avoid JWT requirement.
 */
@Component
@Slf4j
public class AuthServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // Direct call to auth-service (not via gateway — no JWT needed internally)
    @Value("${auth.service.url:http://localhost:8084}")
    private String authServiceUrl;

    /**
     * Creates a login user for a student.
     * Username: firstname.lastname (lowercase, dots)
     * Password: admission number (e.g. ADM2024001)
     */
    public Long createStudentUser(Long studentId, String firstName, String lastName,
                                   String email, String admissionNo) {
        String username = generateUsername(firstName, lastName);
        String password = admissionNo; // default password = admission number

        return createUser(username, password, email, "STUDENT", studentId, null);
    }

    /**
     * Creates a login user for a teacher/faculty.
     * Username: firstname.lastname (lowercase)
     * Password: EMP + facultyId (e.g. EMP001)
     */
    public Long createTeacherUser(Long facultyId, String name, String email) {
        String[] parts = name.trim().split("\\s+");
        String firstName = parts[0];
        String lastName  = parts.length > 1 ? parts[parts.length - 1] : "faculty";
        String username  = generateUsername(firstName, lastName);
        String password  = "EMP" + String.format("%03d", facultyId);

        return createUser(username, password, email, "TEACHER", null, facultyId);
    }

    private Long createUser(String username, String password, String email,
                             String role, Long studentId, Long teacherId) {
        try {
            // Check if username already exists — append number if taken
            String finalUsername = ensureUniqueUsername(username);

            Map<String, Object> body = Map.of(
                "username",  finalUsername,
                "password",  password,
                "email",     email != null ? email : finalUsername + "@successacademy.edu.in",
                "role",      role,
                "studentId", studentId != null ? studentId : 0,
                "teacherId", teacherId != null ? teacherId : 0
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                authServiceUrl + "/auth/register", entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Object id = response.getBody().get("id");
                log.info("✅ User created: username={} role={} userId={}", finalUsername, role, id);
                return id instanceof Number ? ((Number) id).longValue() : null;
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not create auth user for {} {}: {}", role, username, e.getMessage());
        }
        return null;
    }

    private String ensureUniqueUsername(String base) {
        try {
            ResponseEntity<Boolean> resp = restTemplate.getForEntity(
                authServiceUrl + "/auth/exists/" + base, Boolean.class
            );
            if (Boolean.TRUE.equals(resp.getBody())) {
                // Try base + random 2-digit number
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
