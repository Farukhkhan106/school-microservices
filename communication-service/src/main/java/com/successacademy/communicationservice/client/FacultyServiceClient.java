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
public class FacultyServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${faculty.service.url:http://localhost:8086}")
    private String facultyServiceUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FacultyInfo {
        private Long id;
        private String name;
        private String email;
        private String designation;
        private String qualification;
        private String classTeacherOf; // e.g. "10-A"
        private List<String> subjects;
        private Long userId;

        public boolean isPrincipal() {
            return designation != null && designation.toLowerCase().contains("principal");
        }

        public boolean teachesClassSection(String targetClass, String targetSection) {
            if (targetClass == null || targetClass.isBlank()) return false;
            String key = targetClass.trim() + "-" + (targetSection != null ? targetSection.trim().toUpperCase() : "A");

            // Check class teacher responsibility
            if (classTeacherOf != null && classTeacherOf.trim().equalsIgnoreCase(key)) {
                return true;
            }
            // If designation is Principal / Administration, they have broad teaching oversight
            if (isPrincipal()) {
                return true;
            }
            return false;
        }
    }

    private final Map<Long, FacultyInfo> cacheById = new ConcurrentHashMap<>();
    private final Map<Long, FacultyInfo> cacheByUserId = new ConcurrentHashMap<>();

    public FacultyInfo getFacultyById(Long facultyId) {
        if (facultyId == null) return null;
        if (cacheById.containsKey(facultyId)) return cacheById.get(facultyId);

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(facultyServiceUrl + "/faculty/" + facultyId, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                FacultyInfo info = mapToFacultyInfo(resp.getBody());
                if (info != null) {
                    cacheById.put(facultyId, info);
                    if (info.getUserId() != null) cacheByUserId.put(info.getUserId(), info);
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch faculty by id {} from faculty-service: {}", facultyId, e.getMessage());
        }
        return null;
    }

    public FacultyInfo getFacultyByUserId(Long userId) {
        if (userId == null) return null;
        if (cacheByUserId.containsKey(userId)) return cacheByUserId.get(userId);

        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(facultyServiceUrl + "/faculty/user/" + userId, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                FacultyInfo info = mapToFacultyInfo(resp.getBody());
                if (info != null) {
                    cacheByUserId.put(userId, info);
                    if (info.getId() != null) cacheById.put(info.getId(), info);
                    return info;
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch faculty by userId {} from faculty-service: {}", userId, e.getMessage());
        }
        return null;
    }

    public List<FacultyInfo> getAllFaculty() {
        try {
            ResponseEntity<List<Map>> resp = restTemplate.exchange(
                    facultyServiceUrl + "/faculty/all",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                List<FacultyInfo> list = new ArrayList<>();
                for (Map m : resp.getBody()) {
                    FacultyInfo info = mapToFacultyInfo(m);
                    if (info != null) {
                        list.add(info);
                        if (info.getId() != null) cacheById.put(info.getId(), info);
                        if (info.getUserId() != null) cacheByUserId.put(info.getUserId(), info);
                    }
                }
                return list;
            }
        } catch (Exception e) {
            log.warn("Could not fetch all faculty from faculty-service: {}", e.getMessage());
        }
        return List.of();
    }

    private FacultyInfo mapToFacultyInfo(Map m) {
        Long id = toLong(m.get("id"));
        String name = (String) m.get("name");
        String email = (String) m.get("email");
        String desig = (String) m.get("designation");
        String qual = (String) m.get("qualification");
        String ctOf = (String) m.get("classTeacherOf");
        Long uId = toLong(m.get("userId"));

        List<String> subjs = new ArrayList<>();
        Object subjectsObj = m.get("subjects");
        if (subjectsObj instanceof List<?>) {
            for (Object item : (List<?>) subjectsObj) {
                if (item != null) subjs.add(item.toString());
            }
        } else if (subjectsObj instanceof String) {
            String s = (String) subjectsObj;
            for (String part : s.split(",")) {
                if (!part.isBlank()) subjs.add(part.trim());
            }
        }

        return FacultyInfo.builder()
                .id(id)
                .name(name)
                .email(email)
                .designation(desig)
                .qualification(qual)
                .classTeacherOf(ctOf)
                .subjects(subjs)
                .userId(uId)
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
