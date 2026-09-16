package com.successacademy.chatbotservice.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.chatbotservice.ai.FuzzySpellCorrectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolExecutionService {

    private final FuzzySpellCorrectionService spellCorrectionService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${service.student.url:http://localhost:8085}")
    private String studentServiceUrl;

    @Value("${service.faculty.url:http://localhost:8086}")
    private String facultyServiceUrl;

    @Value("${service.attendance.url:http://localhost:8088}")
    private String attendanceServiceUrl;

    @Value("${service.fee.url:http://localhost:8087}")
    private String feeServiceUrl;

    @Value("${service.notice.url:http://localhost:8081}")
    private String noticeServiceUrl;

    @Value("${service.event.url:http://localhost:8082}")
    private String eventServiceUrl;

    @Value("${service.contact.url:http://localhost:8083}")
    private String contactServiceUrl;

    /**
     * Returns list of tools available for a given role.
     */
    public List<ToolDefinition> getAvailableToolsForRole(String role) {
        String r = (role != null ? role.toUpperCase() : "STUDENT");
        List<ToolDefinition> allTools = getAllTools();
        List<ToolDefinition> allowed = new ArrayList<>();
        for (ToolDefinition t : allTools) {
            if (t.getAllowedRoles().contains("ALL") || t.getAllowedRoles().contains(r)) {
                allowed.add(t);
            }
        }
        return allowed;
    }

    private List<ToolDefinition> getAllTools() {
        return List.of(
            // Student Tools
            ToolDefinition.builder()
                .name("getMyProfile")
                .description("Retrieves the authenticated student's profile (name, admission number, class, section, roll number, parents, contact).")
                .allowedRoles(List.of("STUDENT"))
                .build(),

            ToolDefinition.builder()
                .name("getMyAttendance")
                .description("Retrieves the authenticated student's daily attendance records and percentages (Present, Absent, Late). Attendance is daily, not subject-wise.")
                .allowedRoles(List.of("STUDENT"))
                .build(),

            ToolDefinition.builder()
                .name("getMyFees")
                .description("Retrieves the authenticated student's fee summary, total due, amount paid, remaining balance, and receipts.")
                .allowedRoles(List.of("STUDENT"))
                .build(),

            ToolDefinition.builder()
                .name("getMyTimetable")
                .description("Retrieves the daily and weekly timetable / scheduled periods for the student's class and section.")
                .allowedRoles(List.of("STUDENT"))
                .build(),

            // Teacher Tools
            ToolDefinition.builder()
                .name("getMyTeacherProfile")
                .description("Retrieves the authenticated teacher's profile, subjects taught, and designated Class Teacher class if any.")
                .allowedRoles(List.of("TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("getMyClasses")
                .description("Retrieves the classes, sections, and subjects assigned to the authenticated teacher.")
                .allowedRoles(List.of("TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("getMyStudents")
                .description("Retrieves the list of students in the authenticated teacher's assigned class/section.")
                .allowedRoles(List.of("TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("getTeacherSchedule")
                .description("Retrieves the authenticated teacher's daily/weekly teaching timetable and periods.")
                .allowedRoles(List.of("TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("getClassAttendance")
                .description("Retrieves today's attendance status for the teacher's assigned class/section.")
                .allowedRoles(List.of("TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("searchStudent")
                .description("Searches for a student by name or admission number and retrieves their academic and demographic details.")
                .allowedRoles(List.of("ADMIN", "TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("searchTeacher")
                .description("Searches for a teacher/faculty member by name, subject, or department and retrieves their teaching profile and assigned classes.")
                .allowedRoles(List.of("ALL"))
                .build(),

            ToolDefinition.builder()
                .name("getClassStudents")
                .description("Retrieves the complete list of students enrolled in a specific class and section (e.g. Class 10-A).")
                .allowedRoles(List.of("ADMIN", "TEACHER"))
                .build(),

            ToolDefinition.builder()
                .name("getStudentStatistics")
                .description("Retrieves total student enrollment count, active vs inactive counts, and breakdown per class.")
                .allowedRoles(List.of("ADMIN"))
                .build(),

            ToolDefinition.builder()
                .name("getFacultySummary")
                .description("Retrieves total faculty count, active teachers, and subject department distribution.")
                .allowedRoles(List.of("ADMIN"))
                .build(),

            ToolDefinition.builder()
                .name("getAttendanceAnalytics")
                .description("Retrieves all-school daily attendance telemetry: total present, absent, late, and not-marked summary.")
                .allowedRoles(List.of("ADMIN"))
                .build(),

            ToolDefinition.builder()
                .name("getFeeAnalytics")
                .description("Retrieves total revenue collected, outstanding pending fees, and count of cleared vs pending student accounts.")
                .allowedRoles(List.of("ADMIN"))
                .build(),

            ToolDefinition.builder()
                .name("getPendingInquiries")
                .description("Retrieves public contact and admission inquiries that are pending a response.")
                .allowedRoles(List.of("ADMIN"))
                .build(),

            // Public / Shared Tools
            ToolDefinition.builder()
                .name("getLatestNotices")
                .description("Retrieves recent school announcements, circulars, and official notices.")
                .allowedRoles(List.of("ALL"))
                .build(),

            ToolDefinition.builder()
                .name("getUpcomingEvents")
                .description("Retrieves upcoming academic, sports, and cultural school events.")
                .allowedRoles(List.of("ALL"))
                .build()
        );
    }

    /**
     * Executes a tool strictly enforcing the caller's authenticated context.
     */
    public String executeTool(String toolName, Map<String, Object> params, Long userId, String role, Long studentId, Long teacherId) {
        log.info("Executing tool={} for role={}, userId={}, studentId={}, teacherId={}", toolName, role, userId, studentId, teacherId);

        String r = (role != null ? role.toUpperCase() : "STUDENT");

        // Role Permission Guard
        List<ToolDefinition> allowed = getAvailableToolsForRole(r);
        boolean isAllowed = allowed.stream().anyMatch(t -> t.getName().equalsIgnoreCase(toolName));
        if (!isAllowed) {
            log.warn("Unauthorized tool execution attempt: tool={}, role={}", toolName, role);
            return "{\"error\": \"Access denied: You are not authorized to execute this tool.\"}";
        }

        HttpHeaders headers = createHeaders(userId, r, studentId, teacherId);

        try {
            switch (toolName) {
                // ── STUDENT TOOLS ──
                case "getMyProfile":
                    return executeGetMyProfile(studentId, headers);

                case "getMyAttendance":
                    return executeGetMyAttendance(studentId, headers);

                case "getMyFees":
                    return executeGetMyFees(studentId, headers);

                case "getMyTimetable":
                    return executeGetMyTimetable(studentId, headers);

                // ── TEACHER TOOLS ──
                case "getMyTeacherProfile":
                    return executeGetTeacherProfile(userId, teacherId, headers);

                case "getMyClasses":
                    return executeGetTeacherClasses(userId, headers);

                case "getMyStudents":
                    return executeGetTeacherStudents(userId, headers);

                case "getTeacherSchedule":
                    return executeGetTeacherSchedule(userId, headers);

                case "getClassAttendance":
                    return executeGetClassAttendance(userId, headers);

                // ── ADMIN / SHARED LOOKUP TOOLS ──
                case "searchStudent":
                    String searchQ = params != null && params.get("query") != null ? params.get("query").toString() : "";
                    return executeSearchStudent(searchQ, headers);

                case "searchTeacher":
                    String teacherQ = params != null && params.get("query") != null ? params.get("query").toString() : "";
                    return executeSearchTeacher(teacherQ, headers);

                case "getClassStudents":
                    String cName = params != null && params.get("className") != null ? params.get("className").toString() : "10";
                    String sec = params != null && params.get("section") != null ? params.get("section").toString() : null;
                    return executeGetClassStudents(cName, sec, headers);

                case "getStudentStatistics":
                    return executeGetStudentStatistics(headers);

                case "getFacultySummary":
                    return executeGetFacultySummary(headers);

                case "getAttendanceAnalytics":
                    return executeGetAttendanceAnalytics(headers);

                case "getFeeAnalytics":
                    return executeGetFeeAnalytics(headers);

                case "getPendingInquiries":
                    return executeGetPendingInquiries(headers);

                // ── SHARED TOOLS ──
                case "getLatestNotices":
                    return executeGetLatestNotices(headers);

                case "getUpcomingEvents":
                    return executeGetUpcomingEvents(headers);

                default:
                    return "{\"error\": \"Unknown tool name: " + toolName + "\"}";
            }
        } catch (Exception e) {
            log.error("Tool execution failed for tool={}: {}", toolName, e.getMessage());
            return "{\"error\": \"Failed to retrieve ERP data: " + e.getMessage() + "\"}";
        }
    }

    // ── Tool Implementation Details ──

    private String executeGetMyProfile(Long studentId, HttpHeaders headers) {
        if (studentId == null) return "{\"error\": \"No student account linked to this user.\"}";
        String url = studentServiceUrl + "/students/profile/" + studentId;
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    private String executeGetMyAttendance(Long studentId, HttpHeaders headers) {
        if (studentId == null) return "{\"error\": \"No student account linked to this user.\"}";
        String url = attendanceServiceUrl + "/attendance/student/" + studentId + "/summary";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            return res.getBody();
        } catch (Exception e) {
            // Fallback to student logs
            String logUrl = attendanceServiceUrl + "/attendance/student/" + studentId;
            ResponseEntity<String> res = restTemplate.exchange(logUrl, HttpMethod.GET, req, String.class);
            return res.getBody();
        }
    }

    private String executeGetMyFees(Long studentId, HttpHeaders headers) {
        if (studentId == null) return "{\"error\": \"No student account linked to this user.\"}";
        String url = feeServiceUrl + "/fees/records/student/" + studentId;
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    @SuppressWarnings("unchecked")
    private String executeGetMyTimetable(Long studentId, HttpHeaders headers) {
        if (studentId == null) return "{\"error\": \"No student account linked to this user.\"}";
        // 1. Get student class & section
        String profileUrl = studentServiceUrl + "/students/profile/" + studentId;
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<Map> profileRes = restTemplate.exchange(profileUrl, HttpMethod.GET, req, Map.class);
        Map profile = profileRes.getBody();
        if (profile == null) return "{\"error\": \"Could not determine student class.\"}";

        String studentClass = (String) profile.get("studentClass");
        String section = (String) profile.get("section");
        if (studentClass == null) return "{\"error\": \"Class not assigned.\"}";

        String scheduleUrl = String.format("%s/faculty/schedule/class?studentClass=%s&section=%s",
                facultyServiceUrl, studentClass, section != null ? section : "A");
        ResponseEntity<String> scheduleRes = restTemplate.exchange(scheduleUrl, HttpMethod.GET, req, String.class);
        return scheduleRes.getBody();
    }

    private String executeGetTeacherProfile(Long userId, Long teacherId, HttpHeaders headers) {
        String url = facultyServiceUrl + "/faculty/teacher/me";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            return res.getBody();
        } catch (Exception e) {
            if (teacherId != null) {
                String publicUrl = facultyServiceUrl + "/faculty/" + teacherId;
                return restTemplate.exchange(publicUrl, HttpMethod.GET, req, String.class).getBody();
            }
            return "{\"message\": \"Teacher profile active for User ID: " + userId + "\"}";
        }
    }

    private String executeGetTeacherClasses(Long userId, HttpHeaders headers) {
        String url = facultyServiceUrl + "/faculty/teacher/classes";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    @SuppressWarnings("unchecked")
    private String executeGetTeacherStudents(Long userId, HttpHeaders headers) {
        // Get teacher classes first
        String classesUrl = facultyServiceUrl + "/faculty/teacher/classes";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> classesRes = restTemplate.exchange(classesUrl, HttpMethod.GET, req, List.class);
        List classes = classesRes.getBody();
        if (classes == null || classes.isEmpty()) {
            return "{\"message\": \"No classes currently assigned to this teacher.\"}";
        }

        // Fetch students for first assigned class
        String firstClassStr = String.valueOf(classes.get(0));
        String[] parts = firstClassStr.split("-");
        String className = parts[0].trim();
        String section = parts.length > 1 ? parts[1].trim() : "A";

        String studentsUrl = String.format("%s/students/class?studentClass=%s&section=%s",
                studentServiceUrl, className, section);
        ResponseEntity<String> studentsRes = restTemplate.exchange(studentsUrl, HttpMethod.GET, req, String.class);
        return studentsRes.getBody();
    }

    private String executeGetTeacherSchedule(Long userId, HttpHeaders headers) {
        String url = facultyServiceUrl + "/faculty/teacher/schedule";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    @SuppressWarnings("unchecked")
    private String executeGetClassAttendance(Long userId, HttpHeaders headers) {
        // Determine class teacher of
        String classesUrl = facultyServiceUrl + "/faculty/teacher/classes";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> classesRes = restTemplate.exchange(classesUrl, HttpMethod.GET, req, List.class);
        List classes = classesRes.getBody();
        if (classes == null || classes.isEmpty()) {
            return "{\"message\": \"No classes assigned.\"}";
        }

        String firstClassStr = String.valueOf(classes.get(0));
        String[] parts = firstClassStr.split("-");
        String className = parts[0].trim();
        String section = parts.length > 1 ? parts[1].trim() : "A";

        String attUrl = String.format("%s/attendance/class?studentClass=%s&section=%s",
                attendanceServiceUrl, className, section != null ? section : "A");
        ResponseEntity<String> attRes = restTemplate.exchange(attUrl, HttpMethod.GET, req, String.class);
        return attRes.getBody();
    }

    @SuppressWarnings("unchecked")
    private String executeGetStudentStatistics(HttpHeaders headers) {
        String url = studentServiceUrl + "/students";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> res = restTemplate.exchange(url, HttpMethod.GET, req, List.class);
        List students = res.getBody();
        if (students == null) return "{\"totalStudents\": 0}";

        int total = students.size();
        int active = 0;
        Map<String, Integer> perClass = new HashMap<>();

        for (Object obj : students) {
            Map s = (Map) obj;
            String status = (String) s.get("status");
            if ("Active".equalsIgnoreCase(status)) active++;
            String cls = (String) s.get("studentClass");
            if (cls != null) {
                perClass.put(cls, perClass.getOrDefault(cls, 0) + 1);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalStudents", total);
        result.put("activeStudents", active);
        result.put("inactiveStudents", total - active);
        result.put("studentsPerClass", perClass);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"totalStudents\": " + total + ", \"activeStudents\": " + active + "}";
        }
    }

    @SuppressWarnings("unchecked")
    private String executeGetFacultySummary(HttpHeaders headers) {
        String url = facultyServiceUrl + "/faculty/public";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> res = restTemplate.exchange(url, HttpMethod.GET, req, List.class);
        List faculty = res.getBody();
        if (faculty == null) return "{\"totalFaculty\": 0}";

        int total = faculty.size();
        int active = 0;
        Set<String> departments = new HashSet<>();

        for (Object obj : faculty) {
            Map f = (Map) obj;
            String status = (String) f.get("status");
            if ("Active".equalsIgnoreCase(status) || status == null) active++;
            String dept = (String) f.get("department");
            if (dept != null && !dept.isBlank()) departments.add(dept);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalFaculty", total);
        result.put("activeTeachers", active);
        result.put("departments", departments);

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"totalFaculty\": " + total + ", \"activeTeachers\": " + active + "}";
        }
    }

    @SuppressWarnings("unchecked")
    private String executeGetAttendanceAnalytics(HttpHeaders headers) {
        String url = attendanceServiceUrl + "/attendance/all";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> res = restTemplate.exchange(url, HttpMethod.GET, req, List.class);
        List logs = res.getBody();
        if (logs == null) return "{\"totalMarked\": 0}";

        int present = 0;
        int late = 0;
        int absent = 0;

        for (Object obj : logs) {
            Map logItem = (Map) obj;
            String status = (String) logItem.get("status");
            if ("PRESENT".equalsIgnoreCase(status)) present++;
            else if ("LATE".equalsIgnoreCase(status)) late++;
            else if ("ABSENT".equalsIgnoreCase(status)) absent++;
        }

        int totalMarked = present + late + absent;
        double percentage = totalMarked > 0 ? ((double) (present + late) / totalMarked) * 100.0 : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMarkedRecords", totalMarked);
        result.put("presentCount", present);
        result.put("lateCount", late);
        result.put("absentCount", absent);
        result.put("attendancePercentage", Math.round(percentage * 10.0) / 10.0);
        result.put("note", "Attendance is Daily School Attendance (not subject-wise).");

        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"totalMarked\": " + totalMarked + ", \"present\": " + present + ", \"absent\": " + absent + "}";
        }
    }

    private String executeGetFeeAnalytics(HttpHeaders headers) {
        String url = feeServiceUrl + "/fees/stats";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    @SuppressWarnings("unchecked")
    private String executeGetPendingInquiries(HttpHeaders headers) {
        String url = contactServiceUrl + "/contact/all";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> res = restTemplate.exchange(url, HttpMethod.GET, req, List.class);
        List contacts = res.getBody();
        if (contacts == null) return "{\"pendingCount\": 0}";

        int pending = 0;
        for (Object obj : contacts) {
            Map c = (Map) obj;
            String status = (String) c.get("status");
            if (!"Resolved".equalsIgnoreCase(status)) pending++;
        }

        return "{\"totalInquiries\": " + contacts.size() + ", \"pendingInquiries\": " + pending + "}";
    }

    private String executeGetLatestNotices(HttpHeaders headers) {
        String url = noticeServiceUrl + "/notice/public";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    @SuppressWarnings("unchecked")
    private String executeSearchStudent(String query, HttpHeaders headers) {
        if (query == null || query.isBlank()) {
            return "{\"message\": \"Please specify a student name or admission number to search.\"}";
        }
        String url = studentServiceUrl + "/students";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> res = restTemplate.exchange(url, HttpMethod.GET, req, List.class);
        List students = res.getBody();
        if (students == null || students.isEmpty()) {
            return "{\"message\": \"No student records found in database.\"}";
        }

        String q = query.toLowerCase().trim();
        String[] qTokens = q.split("\\s+");
        List<Map<String, Object>> matches = new ArrayList<>();
        Set<Object> matchedIds = new HashSet<>();

        // Stage 1: Exact / Substring & Transliteration Match
        for (Object obj : students) {
            Map<String, Object> s = (Map<String, Object>) obj;
            String fn = String.valueOf(s.getOrDefault("firstName", "")).toLowerCase();
            String ln = String.valueOf(s.getOrDefault("lastName", "")).toLowerCase();
            String fullName = (fn + " " + ln).trim();
            String adm = String.valueOf(s.getOrDefault("admissionNo", "")).toLowerCase();

            if (fullName.contains(q) || (qTokens.length == 1 && (fn.contains(q) || ln.contains(q))) || adm.equalsIgnoreCase(q)) {
                matches.add(0, new HashMap<>(s));
                matchedIds.add(s.get("id"));
                if (matches.size() >= 5) break;
                continue;
            }

            // Transliteration match (e.g. Sweta -> Shweta, Vikas -> Wikas)
            boolean tokenMatch = true;
            for (String tok : qTokens) {
                if (tok.length() >= 3) {
                    String normTok = tok.replace("h", "").replace("v", "w").replace("ee", "i").replace("oo", "u");
                    String normFull = fullName.replace("h", "").replace("v", "w").replace("ee", "i").replace("oo", "u");
                    String normAdm = adm.replace("h", "").replace("v", "w");
                    if (!normFull.contains(normTok) && !normAdm.contains(normTok)) {
                        tokenMatch = false;
                        break;
                    }
                } else {
                    tokenMatch = false;
                    break;
                }
            }
            if (tokenMatch && qTokens.length > 0) {
                matches.add(0, new HashMap<>(s));
                matchedIds.add(s.get("id"));
                if (matches.size() >= 5) break;
            }
        }

        // Stage 2: Deep Levenshtein & Fuzzy Similarity Match (Typo-Tolerance)
        List<Map.Entry<Map<String, Object>, Double>> scoredFuzzy = new ArrayList<>();
        for (Object obj : students) {
            Map<String, Object> s = (Map<String, Object>) obj;
            if (matchedIds.contains(s.get("id"))) continue;

            String fn = String.valueOf(s.getOrDefault("firstName", "")).toLowerCase();
            String ln = String.valueOf(s.getOrDefault("lastName", "")).toLowerCase();
            String fullName = (fn + " " + ln).trim();
            String adm = String.valueOf(s.getOrDefault("admissionNo", "")).toLowerCase();

            double score = spellCorrectionService.calculateSimilarity(q, fullName);

            int matchedTokenCount = 0;
            for (String tok : qTokens) {
                if (tok.length() < 3) continue;
                double simFn = spellCorrectionService.calculateSimilarity(tok, fn);
                double simLn = spellCorrectionService.calculateSimilarity(tok, ln);
                int distFn = spellCorrectionService.calculateLevenshteinDistance(tok, fn);
                int distLn = spellCorrectionService.calculateLevenshteinDistance(tok, ln);

                if (simFn >= 0.70 || simLn >= 0.70 || (distFn <= 2 && fn.length() >= 4) || (distLn <= 2 && ln.length() >= 4)) {
                    matchedTokenCount++;
                    score += Math.max(simFn, simLn);
                }
            }

            if (score >= 0.65 || (matchedTokenCount > 0 && matchedTokenCount >= qTokens.length)) {
                scoredFuzzy.add(new AbstractMap.SimpleEntry<>(new HashMap<>(s), score));
            }
        }

        // Sort fuzzy matches by score descending
        scoredFuzzy.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (Map.Entry<Map<String, Object>, Double> entry : scoredFuzzy) {
            matches.add(entry.getKey());
            matchedIds.add(entry.getKey().get("id"));
            if (matches.size() >= 5) break;
        }

        if (matches.isEmpty()) {
            return "{\"message\": \"No student found matching '" + query + "'.\"}";
        }

        // Enrich up to 3 matches with live attendance & fee records
        for (int i = 0; i < Math.min(3, matches.size()); i++) {
            Map<String, Object> s = matches.get(i);
            Object idObj = s.get("id");
            if (idObj instanceof Number) {
                Long sid = ((Number) idObj).longValue();
                // 1. Fetch live attendance summary
                try {
                    String attUrl = attendanceServiceUrl + "/attendance/student/" + sid + "/summary";
                    ResponseEntity<Map> attRes = restTemplate.exchange(attUrl, HttpMethod.GET, req, Map.class);
                    if (attRes.getBody() != null) {
                        s.put("attendanceSummary", attRes.getBody());
                    }
                } catch (Exception e) {
                    log.debug("No attendance summary for student id {}: {}", sid, e.getMessage());
                }

                // 2. Fetch live fee records
                try {
                    String feeUrl = feeServiceUrl + "/fees/records/student/" + sid;
                    ResponseEntity<List> feeRes = restTemplate.exchange(feeUrl, HttpMethod.GET, req, List.class);
                    if (feeRes.getBody() != null) {
                        s.put("feeRecords", feeRes.getBody());
                    }
                } catch (Exception e) {
                    log.debug("No fee records for student id {}: {}", sid, e.getMessage());
                }
            }
        }

        try {
            return objectMapper.writeValueAsString(matches);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize student results\"}";
        }
    }

    @SuppressWarnings("unchecked")
    private String executeSearchTeacher(String query, HttpHeaders headers) {
        if (query == null || query.isBlank()) {
            return "{\"message\": \"Please specify a teacher name, subject, or department to search.\"}";
        }
        String url = facultyServiceUrl + "/faculty/all";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<List> res;
        try {
            res = restTemplate.exchange(url, HttpMethod.GET, req, List.class);
        } catch (Exception e) {
            String pubUrl = facultyServiceUrl + "/faculty/public";
            res = restTemplate.exchange(pubUrl, HttpMethod.GET, req, List.class);
        }
        List faculty = res.getBody();
        if (faculty == null || faculty.isEmpty()) {
            return "{\"message\": \"No faculty records found in database.\"}";
        }

        String q = query.toLowerCase().trim();
        String[] qTokens = q.split("\\s+");
        List<Map<String, Object>> matches = new ArrayList<>();
        Set<Object> matchedIds = new HashSet<>();

        // Stage 1: Exact / Substring match
        for (Object obj : faculty) {
            Map<String, Object> f = (Map<String, Object>) obj;
            String name = String.valueOf(f.getOrDefault("name", "")).toLowerCase();
            String dept = String.valueOf(f.getOrDefault("department", "")).toLowerCase();
            String desig = String.valueOf(f.getOrDefault("designation", "")).toLowerCase();
            String subjects = String.valueOf(f.getOrDefault("subjects", "")).toLowerCase();
            String ct = String.valueOf(f.getOrDefault("classTeacherOf", "")).toLowerCase();
            if (name.contains(q) || dept.contains(q) || desig.contains(q) || subjects.contains(q) || ct.contains(q)) {
                matches.add(f);
                matchedIds.add(f.get("id"));
                if (matches.size() >= 5) break;
            }
        }

        // Stage 2: Fuzzy Levenshtein match for teacher name / department / subject typos
        if (matches.isEmpty()) {
            for (Object obj : faculty) {
                Map<String, Object> f = (Map<String, Object>) obj;
                if (matchedIds.contains(f.get("id"))) continue;

                String name = String.valueOf(f.getOrDefault("name", "")).toLowerCase();
                String dept = String.valueOf(f.getOrDefault("department", "")).toLowerCase();
                String subjects = String.valueOf(f.getOrDefault("subjects", "")).toLowerCase();

                boolean isFuzzy = false;
                if (spellCorrectionService.calculateSimilarity(q, name) >= 0.65) {
                    isFuzzy = true;
                }

                if (!isFuzzy) {
                    for (String tok : qTokens) {
                        if (tok.length() < 3) continue;
                        if (spellCorrectionService.calculateSimilarity(tok, name) >= 0.70
                                || spellCorrectionService.calculateSimilarity(tok, dept) >= 0.75
                                || spellCorrectionService.calculateSimilarity(tok, subjects) >= 0.75
                                || spellCorrectionService.calculateLevenshteinDistance(tok, name) <= 2) {
                            isFuzzy = true;
                            break;
                        }
                    }
                }

                if (isFuzzy) {
                    matches.add(f);
                    matchedIds.add(f.get("id"));
                    if (matches.size() >= 5) break;
                }
            }
        }

        if (matches.isEmpty()) {
            return "{\"message\": \"No teacher found matching '" + query + "'.\"}";
        }

        try {
            return objectMapper.writeValueAsString(matches);
        } catch (Exception e) {
            return "{\"error\": \"Failed to serialize faculty results\"}";
        }
    }

    @SuppressWarnings("unchecked")
    private String executeGetClassStudents(String className, String section, HttpHeaders headers) {
        String url;
        if (section != null && !section.isBlank()) {
            url = String.format("%s/students/class?studentClass=%s&section=%s", studentServiceUrl, className, section);
        } else {
            url = String.format("%s/students/class?studentClass=%s", studentServiceUrl, className);
        }
        HttpEntity<Void> req = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
            return res.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch students for class {}-{}: {}", className, section, e.getMessage());
            return "{\"message\": \"Could not retrieve students for Class " + className + (section != null ? "-" + section : "") + "\"}";
        }
    }

    private String executeGetUpcomingEvents(HttpHeaders headers) {
        String url = eventServiceUrl + "/event/public";
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, req, String.class);
        return res.getBody();
    }

    private HttpHeaders createHeaders(Long userId, String role, Long studentId, Long teacherId) {
        HttpHeaders headers = new HttpHeaders();
        if (userId != null) headers.set("X-User-Id", String.valueOf(userId));
        if (role != null) headers.set("X-User-Role", role);
        if (studentId != null) headers.set("X-Student-Id", String.valueOf(studentId));
        if (teacherId != null) headers.set("X-Teacher-Id", String.valueOf(teacherId));
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
