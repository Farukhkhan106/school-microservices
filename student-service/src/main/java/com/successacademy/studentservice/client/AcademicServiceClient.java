package com.successacademy.studentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Client to interact with academic-service to automatically synchronize
 * new admissions and student placement updates into the active Academic Session.
 */
@Component
@Slf4j
public class AcademicServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${academic.service.url:http://localhost:8093}")
    private String academicServiceUrl;

    @Value("${app.gateway.internal-secret:${GATEWAY_INTERNAL_SECRET:success-academy-secure-internal-gateway-token-2026}}")
    private String internalSecret;

    /**
     * Enrolls the newly admitted student into the school's current active Academic Session.
     */
    public void enrollInActiveSession(Long studentId, String studentName, String admissionNo,
                                      String studentClass, String section, String rollNo, String tenantId) {
        if (studentId == null) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Secret", internalSecret);
            headers.set("X-User-Role", "ADMIN");
            headers.set("X-Tenant-Id", tenantId != null ? tenantId : "default");

            // 1. Resolve current active session
            HttpEntity<?> getEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> sessionResp = restTemplate.exchange(
                    academicServiceUrl + "/academic/sessions/active",
                    HttpMethod.GET,
                    getEntity,
                    Map.class
            );

            if (sessionResp.getStatusCode().is2xxSuccessful() && sessionResp.getBody() != null) {
                Object idObj = sessionResp.getBody().get("id");
                if (idObj instanceof Number) {
                    Long sessionId = ((Number) idObj).longValue();

                    Map<String, Object> enrollPayload = Map.of(
                            "sessionId", sessionId,
                            "studentId", studentId,
                            "studentName", studentName != null ? studentName : "",
                            "admissionNo", admissionNo != null ? admissionNo : "",
                            "studentClass", studentClass != null ? studentClass : "1",
                            "section", section != null ? section : "A",
                            "rollNo", rollNo != null ? rollNo : "",
                            "status", "ACTIVE"
                    );

                    HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(enrollPayload, headers);
                    restTemplate.postForEntity(
                            academicServiceUrl + "/academic/sessions/enroll",
                            postEntity,
                            Map.class
                    );
                    log.info("✅ Student {} successfully enrolled into active academic session {}", studentId, sessionId);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not auto-enroll student {} into active academic session: {}", studentId, e.getMessage());
        }
    }
}
