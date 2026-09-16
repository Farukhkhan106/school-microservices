package com.successacademy.attendanceservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class FacultyServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${faculty.service.url:http://localhost:8086}")
    private String facultyServiceUrl;

    @SuppressWarnings("unchecked")
    public boolean hasClassTeacherAccess(Long userId, String studentClass, String section) {
        if (userId == null) return false;
        try {
            String url = String.format("%s/faculty/internal/has-access?userId=%d&studentClass=%s&section=%s",
                    facultyServiceUrl, userId, studentClass, section != null ? section : "");
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp != null && Boolean.TRUE.equals(resp.get("classTeacher"))) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Could not verify class teacher access for userId={}: {}", userId, e.getMessage());
        }
        return false;
    }
}
