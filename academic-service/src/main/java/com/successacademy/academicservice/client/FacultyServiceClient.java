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

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class FacultyServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${faculty.service.url:http://localhost:8086}")
    private String facultyServiceUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeachingAssignmentDto {
        private Long id;
        private Long teacherId;
        private String teacherName;
        private String studentClass;
        private String section;
        private String subject;
        private String status;
    }

    public List<TeachingAssignmentDto> getTeacherAssignments(Long teacherId) {
        if (teacherId == null) {
            return Collections.emptyList();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Role", "ADMIN"); // Internal trusted service call
            HttpEntity<?> entity = new HttpEntity<>(headers);

            String url = facultyServiceUrl + "/faculty/assignments/teacher/" + teacherId;
            ResponseEntity<List<TeachingAssignmentDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<TeachingAssignmentDto>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Failed to fetch teaching assignments for teacherId={}: {}", teacherId, e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Checks if teacher has an ACTIVE assignment for the given class, section, and subject.
     */
    public boolean isTeacherAssigned(Long teacherId, String studentClass, String section, String subject) {
        if (teacherId == null || studentClass == null || section == null || subject == null) {
            return false;
        }

        List<TeachingAssignmentDto> assignments = getTeacherAssignments(teacherId);
        return assignments.stream()
                .filter(a -> a.getStatus() == null || "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .anyMatch(a ->
                        a.getStudentClass() != null && a.getStudentClass().trim().equalsIgnoreCase(studentClass.trim()) &&
                        a.getSection() != null && a.getSection().trim().equalsIgnoreCase(section.trim()) &&
                        a.getSubject() != null && a.getSubject().trim().equalsIgnoreCase(subject.trim())
                );
    }
}
