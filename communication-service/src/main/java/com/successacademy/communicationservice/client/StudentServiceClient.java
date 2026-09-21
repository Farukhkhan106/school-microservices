package com.successacademy.communicationservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    public static class StudentInfo {
        private Long id;
        private String firstName;
        private String lastName;
        private String studentClass;
        private String section;
        private String email;
        private String status;

        public String getFullName() {
            return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
        }

        public String getClassSectionKey() {
            if (studentClass == null || studentClass.isBlank()) return null;
            String sec = (section != null && !section.isBlank()) ? section.trim().toUpperCase() : "A";
            return studentClass.trim() + "-" + sec;
        }
    }

    private final Map<Long, StudentInfo> cacheById = new ConcurrentHashMap<>();

    public StudentInfo getStudentById(Long studentId) {
        if (studentId == null) return null;
        if (cacheById.containsKey(studentId)) return cacheById.get(studentId);

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(studentServiceUrl + "/students/" + studentId, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                StudentInfo info = mapToStudentInfo(resp.getBody());
                if (info != null) {
                    cacheById.put(studentId, info);
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch student by id {} from student-service: {}", studentId, e.getMessage());
        }
        return null;
    }

    public List<StudentInfo> getAllStudents() {
        try {
            ResponseEntity<List<Map>> resp = restTemplate.exchange(
                    studentServiceUrl + "/students",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<StudentInfo> list = new ArrayList<>();
                for (Map m : resp.getBody()) {
                    StudentInfo info = mapToStudentInfo(m);
                    if (info != null) {
                        list.add(info);
                        if (info.getId() != null) cacheById.put(info.getId(), info);
                    }
                }
                return list;
            }
        } catch (Exception e) {
            log.warn("Could not fetch all students from student-service: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Discovers all real class-section pairs from existing students in the database.
     * e.g. ["10-A", "9-B"]
     */
    public Set<String> getActiveRealClassSections() {
        Set<String> classSections = new TreeSet<>();
        List<StudentInfo> students = getAllStudents();
        for (StudentInfo s : students) {
            String key = s.getClassSectionKey();
            if (key != null && !key.isBlank()) {
                classSections.add(key);
            }
        }
        return classSections;
    }

    private StudentInfo mapToStudentInfo(Map m) {
        Long id = toLong(m.get("id"));
        String fName = (String) m.get("firstName");
        String lName = (String) m.get("lastName");
        String sClass = (String) m.get("studentClass");
        String sec = (String) m.get("section");
        String email = (String) m.get("email");
        String status = (String) m.get("status");

        return StudentInfo.builder()
                .id(id)
                .firstName(fName)
                .lastName(lName)
                .studentClass(sClass)
                .section(sec)
                .email(email)
                .status(status)
                .build();
    }

    private Long toLong(Object val) {
        if (val instanceof Number) return ((Number) val).longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (Exception ignored) {}
        }
        return null;
    }
}
