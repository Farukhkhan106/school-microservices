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
    public List<String> getAllowedClasses(Long userId, Long teacherId) {
        if (userId == null && teacherId == null) return Collections.emptyList();
        try {
            StringBuilder sb = new StringBuilder(facultyServiceUrl).append("/faculty/internal/allowed-classes?");
            if (userId != null) sb.append("userId=").append(userId).append("&");
            if (teacherId != null) sb.append("teacherId=").append(teacherId);
            List<String> list = restTemplate.getForObject(sb.toString(), List.class);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not retrieve allowed classes for teacher userId={}, teacherId={}: {}", userId, teacherId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> getAllowedClasses(Long userId) {
        return getAllowedClasses(userId, null);
    }
}
