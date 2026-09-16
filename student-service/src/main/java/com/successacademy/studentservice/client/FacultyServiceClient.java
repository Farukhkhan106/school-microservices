package com.successacademy.studentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class FacultyServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${faculty.service.url:http://localhost:8086}")
    private String facultyServiceUrl;

    @SuppressWarnings("unchecked")
    public List<String> getAllowedClasses(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            String url = facultyServiceUrl + "/faculty/internal/allowed-classes?userId=" + userId;
            List<String> list = restTemplate.getForObject(url, List.class);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not retrieve allowed classes for teacher userId={}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
