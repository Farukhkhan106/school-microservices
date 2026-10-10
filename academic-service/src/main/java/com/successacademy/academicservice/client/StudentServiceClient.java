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
public class StudentServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${student.service.url:http://localhost:8085}")
    private String studentServiceUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentInfoDto {
        private Long id;
        private String admissionNo;
        private String firstName;
        private String lastName;
        private String studentClass;
        private String section;
        private String rollNo;
        private String status;
    }

    public StudentInfoDto getStudentById(Long studentId) {
        if (studentId == null) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN"); // Internal trusted service call
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = studentServiceUrl + "/students/" + studentId;
            ResponseEntity<StudentInfoDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    StudentInfoDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to fetch student info for studentId={}: {}", studentId, e.getMessage());
        }
        return null;
    }

    public java.util.List<StudentInfoDto> getStudentsByClassAndSection(String studentClass, String section) {
        if (studentClass == null || studentClass.isBlank()) {
            return java.util.Collections.emptyList();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN"); // Internal trusted service call
            HttpEntity<?> entity = new HttpEntity<>(headers);

            StringBuilder url = new StringBuilder(studentServiceUrl + "/students/class?studentClass=" + studentClass.trim());
            if (section != null && !section.isBlank()) {
                url.append("&section=").append(section.trim());
            }

            org.springframework.core.ParameterizedTypeReference<java.util.List<StudentInfoDto>> typeRef =
                    new org.springframework.core.ParameterizedTypeReference<>() {};

            ResponseEntity<java.util.List<StudentInfoDto>> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.GET,
                    entity,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to fetch students for class={}-section={}: {}", studentClass, section, e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    public java.util.List<StudentInfoDto> getAllStudents() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = studentServiceUrl + "/students";
            org.springframework.core.ParameterizedTypeReference<java.util.List<StudentInfoDto>> typeRef =
                    new org.springframework.core.ParameterizedTypeReference<>() {};

            ResponseEntity<java.util.List<StudentInfoDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    typeRef
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to fetch all students: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    public boolean updateStudentPlacement(Long studentId, String studentClass, String section, String rollNo, String status) {
        if (studentId == null) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN");
            HttpEntity<?> entity = new HttpEntity<>(headers);

            StringBuilder url = new StringBuilder(studentServiceUrl + "/students/" + studentId + "/academic-placement?");
            if (studentClass != null) url.append("studentClass=").append(studentClass).append("&");
            if (section != null) url.append("section=").append(section).append("&");
            if (rollNo != null) url.append("rollNo=").append(rollNo).append("&");
            if (status != null) url.append("status=").append(status).append("&");

            ResponseEntity<Void> response = restTemplate.exchange(
                    url.toString(),
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Failed to update placement for studentId={}: {}", studentId, e.getMessage());
            return false;
        }
    }
}

