package com.successacademy.academicservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class AttendanceServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${attendance.service.url:http://localhost:8088}")
    private String attendanceServiceUrl;

    @Value("${app.gateway.internal-secret:${INTERNAL_GATEWAY_SECRET:success-academy-secure-internal-gateway-token-2026}}")
    private String internalGatewaySecret;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceSummaryDto {
        private Long studentId;
        private long totalDays;
        private long presentDays;
        private long absentDays;
        private long lateDays;
        private double overallPercentage;
    }

    public AttendanceSummaryDto getStudentAttendanceSummary(Long studentId) {
        if (studentId == null) {
            return AttendanceSummaryDto.builder().build();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN");
            headers.set("X-Internal-Secret", internalGatewaySecret);
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = attendanceServiceUrl + "/attendance/student/" + studentId + "/summary";
            ResponseEntity<AttendanceSummaryDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AttendanceSummaryDto.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve real attendance for studentId={}: {}. Falling back to zero-state.", studentId, e.getMessage());
        }
        return AttendanceSummaryDto.builder()
                .studentId(studentId)
                .totalDays(0)
                .presentDays(0)
                .absentDays(0)
                .lateDays(0)
                .overallPercentage(0.0)
                .build();
    }
}
