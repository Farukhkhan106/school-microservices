package com.successacademy.academicservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class FeeServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fee.service.url:http://localhost:8087}")
    private String feeServiceUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentFeeSummaryDto {
        private Long studentId;
        private String studentName;
        private String studentClass;
        private String section;
        private String academicYear;
        private String feePlanStatus;
        private String paymentStatus;
        private BigDecimal standardFeeAmount;
        private BigDecimal totalAdjustmentAmount;
        private BigDecimal finalPayableAmount;
        private BigDecimal paidAmount;
        private BigDecimal outstandingBalance;
        private Long planId;
        private Long feeRecordId;
    }

    public List<StudentFeeSummaryDto> getStudentFeeSummaries(String academicYear) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN"); // Trusted internal service call
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = feeServiceUrl + "/fees/plans/all";
            if (academicYear != null && !academicYear.isBlank()) {
                url += "?academicYear=" + academicYear.trim();
            }

            ParameterizedTypeReference<List<StudentFeeSummaryDto>> typeRef = new ParameterizedTypeReference<>() {};
            ResponseEntity<List<StudentFeeSummaryDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch fee summaries from fee-service (url={}, year={}): {}", feeServiceUrl, academicYear, e.getMessage());
        }
        return Collections.emptyList();
    }
}
