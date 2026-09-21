package com.successacademy.chatbotservice.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.chatbotservice.dto.ChatMessageDto;
import com.successacademy.chatbotservice.tools.ToolExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeterministicToolFallbackProvider {

    private final ToolExecutionService toolExecutionService;
    private final FuzzySpellCorrectionService spellCorrectionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIResponse handleQuery(String userMessage,
                                  List<ChatMessageDto> history,
                                  Long userId,
                                  String role,
                                  Long studentId,
                                  Long teacherId) {

        String rawQuery = userMessage.toLowerCase().trim();
        String query = spellCorrectionService.normalizeAndCorrect(rawQuery);
        String r = (role != null ? role.toUpperCase() : "STUDENT");

        log.info("Processing fallback query: role={}, userId={}, raw='{}', norm='{}'", r, userId, userMessage, query);

        // 1. Check for write operations attempt (Prevent natural-language modifications)
        if (isWriteOperationAttempt(rawQuery) || isWriteOperationAttempt(query)) {
            return AIResponse.builder()
                .content("I am an information assistant and read-only for ERP data. Modifications must be performed through the authorized ERP screens.")
                .fallbackUsed(true)
                .build();
        }

        // 2. Check for IDOR attempts / Asking for another student's data (Students only)
        if ("STUDENT".equals(r) && (isOtherStudentQuery(rawQuery) || isOtherStudentQuery(query))) {
            return AIResponse.builder()
                .content("I can only provide information for your own authenticated student account.")
                .fallbackUsed(true)
                .build();
        }

        // 3. Conversational Greetings & Help (English / Hindi / Hinglish)
        if (spellCorrectionService.isGreeting(rawQuery) || spellCorrectionService.isGreeting(query)) {
            return formatGreeting(r);
        }

        // ── STUDENT ROLE QUERY ROUTING ──
        if ("STUDENT".equals(r)) {
            if (query.contains("fee") || query.contains("pay") || query.contains("due") || query.contains("balance") || query.contains("receipt") || query.contains("rupee") || query.contains("money") || rawQuery.contains("fe") || rawQuery.contains("fess")) {
                String toolResult = toolExecutionService.executeTool("getMyFees", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatStudentFees(toolResult);
            }
            if (query.contains("attend") || query.contains("absent") || query.contains("present") || query.contains("late") || query.contains("roll call") || query.contains("hazri") || rawQuery.contains("attend")) {
                String toolResult = toolExecutionService.executeTool("getMyAttendance", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatStudentAttendance(toolResult);
            }
            if (query.contains("timetable") || query.contains("schedule") || query.contains("period") || query.contains("class today") || query.contains("subject") || rawQuery.contains("shedul") || rawQuery.contains("timetbl")) {
                String toolResult = toolExecutionService.executeTool("getMyTimetable", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatStudentTimetable(toolResult);
            }
            if (query.contains("details") || query.contains("profile") || query.contains("admission") || query.contains("roll") || query.contains("parent") || query.contains("father") || query.contains("mother") || query.contains("who am i") || query.contains("my details") || query.contains("section")) {
                String toolResult = toolExecutionService.executeTool("getMyProfile", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatStudentProfile(toolResult);
            }
        }

        // ── TEACHER & ADMIN SMART ENTITY EXTRACTION & INTENT ROUTING ──
        if ("ADMIN".equals(r) || "TEACHER".equals(r)) {
            String entityTerm = cleanEntitySearchTerm(query);
            if (entityTerm.isBlank() || entityTerm.equals("all") || entityTerm.equals("total") || entityTerm.equals("school")) {
                entityTerm = cleanEntitySearchTerm(rawQuery);
            }
            ClassSectionInfo classInfo = extractClassAndSection(query);
            if (classInfo == null) {
                classInfo = extractClassAndSection(rawQuery);
            }

            boolean hasEntity = !entityTerm.isBlank() && !entityTerm.equals("all") && !entityTerm.equals("total") && !entityTerm.equals("school");
            boolean isTeacherIntent = query.contains("teacher") || rawQuery.contains("teachr") || rawQuery.contains("techr") || rawQuery.contains("faculty") || rawQuery.contains("prof") || rawQuery.contains("sir") || rawQuery.contains("madam");
            boolean isAttendanceIntent = query.contains("attendance") || rawQuery.contains("attendece") || rawQuery.contains("attendence") || rawQuery.contains("atendance") || rawQuery.contains("attend") || rawQuery.contains("absent") || rawQuery.contains("present") || rawQuery.contains("roll call") || rawQuery.contains("hazri");
            boolean isFeeIntent = query.contains("fees") || rawQuery.contains("fee") || rawQuery.contains("fe") || rawQuery.contains("fess") || rawQuery.contains("due") || rawQuery.contains("balance") || rawQuery.contains("financial") || rawQuery.contains("ledger") || rawQuery.contains("paisa");
            boolean isClassRosterIntent = classInfo != null && (entityTerm.isBlank() || query.contains("student") || rawQuery.contains("stuent") || rawQuery.contains("studnt") || rawQuery.contains("name") || rawQuery.contains("list") || rawQuery.contains("who") || rawQuery.contains("show") || rawQuery.contains("class") || rawQuery.contains("roster") || rawQuery.contains("bacche"));

            // 1. Specific Teacher Search (e.g., "information about priya teacher", "who is teachr mehta")
            if (isTeacherIntent && hasEntity) {
                String toolResult = toolExecutionService.executeTool("searchTeacher", Map.of("query", entityTerm), userId, r, studentId, teacherId);
                return formatTeacherSearchResults(toolResult, entityTerm);
            }

            // 2. Specific Student Attendance (e.g., "give me attendece information mayank agarwal", "attendance of rahul class 10-a")
            if (isAttendanceIntent && hasEntity) {
                String toolResult = toolExecutionService.executeTool("searchStudent", Map.of("query", entityTerm), userId, r, studentId, teacherId);
                return formatStudentAttendanceRecord(toolResult, entityTerm);
            }

            // 3. Specific Student Fee (e.g., "fee details of student amit singh", "fe of stuent rahul shrma")
            if (isFeeIntent && hasEntity && "ADMIN".equals(r)) {
                String toolResult = toolExecutionService.executeTool("searchStudent", Map.of("query", entityTerm), userId, r, studentId, teacherId);
                return formatStudentFeeRecord(toolResult, entityTerm);
            }

            // 4. Class Student Roster (e.g., "name of stuent of clas 10-a", "students in class 9-B")
            if (isClassRosterIntent && !hasEntity) {
                Map<String, Object> params = new HashMap<>();
                params.put("className", classInfo.studentClass);
                if (classInfo.section != null) {
                    params.put("section", classInfo.section);
                }
                String toolResult = toolExecutionService.executeTool("getClassStudents", params, userId, r, studentId, teacherId);
                return formatClassStudentsList(toolResult, classInfo.studentClass, classInfo.section);
            }

            // 5. Specific Student Profile (e.g., "details of student ravi joshi", "about maynk agrawal", "who is ADM2024045")
            if (hasEntity) {
                String toolResult = toolExecutionService.executeTool("searchStudent", Map.of("query", entityTerm), userId, r, studentId, teacherId);
                return formatStudentSearchResults(toolResult, entityTerm);
            }
        }

        // ── TEACHER ROLE SPECIFIC TOOLS ──
        if ("TEACHER".equals(r)) {
            if (query.contains("timetable") || query.contains("schedule") || query.contains("period") || rawQuery.contains("today's class") || rawQuery.contains("teaching") || rawQuery.contains("shedul")) {
                String toolResult = toolExecutionService.executeTool("getTeacherSchedule", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatTeacherSchedule(toolResult);
            }
            if (query.contains("student") || rawQuery.contains("stuent") || rawQuery.contains("roster") || rawQuery.contains("list")) {
                String toolResult = toolExecutionService.executeTool("getMyStudents", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatTeacherStudents(toolResult);
            }
            if (query.contains("class") || rawQuery.contains("assigned") || rawQuery.contains("subject")) {
                String toolResult = toolExecutionService.executeTool("getMyClasses", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatTeacherClasses(toolResult);
            }
            if (query.contains("attendance") || rawQuery.contains("attend") || rawQuery.contains("absent") || rawQuery.contains("present") || rawQuery.contains("roll call")) {
                String toolResult = toolExecutionService.executeTool("getClassAttendance", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatClassAttendance(toolResult);
            }
            if (query.contains("profile") || query.contains("details") || rawQuery.contains("my detail") || rawQuery.contains("who am i")) {
                String toolResult = toolExecutionService.executeTool("getMyTeacherProfile", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatTeacherProfile(toolResult);
            }
        }

        // ── ADMIN ROLE ANALYTICS TOOLS ──
        if ("ADMIN".equals(r)) {
            if (query.contains("fees") || query.contains("analytics") || rawQuery.contains("collection") || rawQuery.contains("revenue") || rawQuery.contains("pending amount") || rawQuery.contains("financial") || rawQuery.contains("fe")) {
                String toolResult = toolExecutionService.executeTool("getFeeAnalytics", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatAdminFees(toolResult);
            }
            if (query.contains("attendance") || rawQuery.contains("attend") || rawQuery.contains("absent") || rawQuery.contains("present") || rawQuery.contains("percentage") || rawQuery.contains("roll call")) {
                String toolResult = toolExecutionService.executeTool("getAttendanceAnalytics", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatAdminAttendance(toolResult);
            }
            if (query.contains("student") || query.contains("analytics") || rawQuery.contains("stuent") || rawQuery.contains("enroll") || rawQuery.contains("strength") || rawQuery.contains("active student") || rawQuery.contains("total student") || rawQuery.contains("count")) {
                String toolResult = toolExecutionService.executeTool("getStudentStatistics", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatAdminStudents(toolResult);
            }
            if (query.contains("teacher") || rawQuery.contains("faculty") || rawQuery.contains("teachr") || rawQuery.contains("staff") || rawQuery.contains("active teacher")) {
                String toolResult = toolExecutionService.executeTool("getFacultySummary", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatAdminFaculty(toolResult);
            }
            if (query.contains("inquiry") || rawQuery.contains("inquir") || rawQuery.contains("enquir") || rawQuery.contains("message") || rawQuery.contains("contact")) {
                String toolResult = toolExecutionService.executeTool("getPendingInquiries", Collections.emptyMap(), userId, r, studentId, teacherId);
                return formatAdminInquiries(toolResult);
            }
        }

        // ── SHARED (ALL ROLES) ROUTING ──
        if (query.contains("notice") || rawQuery.contains("notis") || rawQuery.contains("notce") || rawQuery.contains("announcement") || rawQuery.contains("circular") || rawQuery.contains("news") || rawQuery.contains("suchna")) {
            String toolResult = toolExecutionService.executeTool("getLatestNotices", Collections.emptyMap(), userId, r, studentId, teacherId);
            return formatNotices(toolResult);
        }
        if (query.contains("event") || rawQuery.contains("evnt") || rawQuery.contains("holiday") || rawQuery.contains("function") || rawQuery.contains("competition") || rawQuery.contains("upcoming")) {
            String toolResult = toolExecutionService.executeTool("getUpcomingEvents", Collections.emptyMap(), userId, r, studentId, teacherId);
            return formatEvents(toolResult);
        }

        // Default response when query is outside ERP tools and AI is unavailable
        return AIResponse.builder()
            .content("I am your Success Academy AI Assistant. You can ask me about:\n- **Attendance** (e.g. `attendance of Mayank Agarwal`)\n- **Student Details** (e.g. `details of student Rahul Sharma`)\n- **Fees & Ledger** (e.g. `fee balance of student Priya`)\n- **Class Roster** (e.g. `students of Class 10-A`)\n- **Teacher Profile** (e.g. `information about Priya teacher`)\n- **School Notices & Events**")
            .fallbackUsed(true)
            .build();
    }

    private boolean isWriteOperationAttempt(String query) {
        return query.startsWith("mark ") || query.startsWith("add ") || query.startsWith("create ") ||
               query.startsWith("update ") || query.startsWith("delete ") || query.startsWith("remove ") ||
               query.contains("change password") || query.contains("set attendance") || query.contains("pay fee");
    }

    private boolean isOtherStudentQuery(String query) {
        return query.contains("rahul") || query.contains("priya") || query.contains("arjun") ||
               query.contains("other student") || query.contains("another student") || query.contains("someone else");
    }

    // ── Response Formatters using Live Tool Output ──

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentFees(String json) {
        try {
            List<Map<String, Object>> records = objectMapper.readValue(json, new TypeReference<>() {});
            if (records.isEmpty()) {
                return AIResponse.builder()
                    .content("You currently have no fee records assigned in the ledger.")
                    .toolUsed("getMyFees")
                    .fallbackUsed(true)
                    .build();
            }

            double totalAmount = 0;
            double totalPaid = 0;
            String status = "Unpaid";
            String dueDate = "";

            for (Map<String, Object> r : records) {
                totalAmount += ((Number) r.getOrDefault("amount", 0)).doubleValue();
                totalPaid += ((Number) r.getOrDefault("paidAmount", 0)).doubleValue();
                status = (String) r.getOrDefault("status", "Unpaid");
                dueDate = (String) r.getOrDefault("dueDate", "");
            }

            double balance = Math.max(0, totalAmount - totalPaid);
            NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            StringBuilder sb = new StringBuilder();
            sb.append("**Fee Summary for Your Account:**\n\n");
            sb.append("- **Total Fees:** ").append(inr.format(totalAmount)).append("\n");
            sb.append("- **Amount Paid:** ").append(inr.format(totalPaid)).append("\n");
            sb.append("- **Remaining Balance:** ").append(inr.format(balance)).append("\n");
            sb.append("- **Payment Status:** ").append(status).append("\n");
            if (!dueDate.isBlank()) {
                sb.append("- **Due Date:** ").append(dueDate).append("\n");
            }

            return AIResponse.builder().content(sb.toString()).toolUsed("getMyFees").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Could not format fee details: " + json).toolUsed("getMyFees").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentAttendance(String json) {
        try {
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<>() {});
            int total = ((Number) data.getOrDefault("totalDays", data.getOrDefault("totalMarked", 0))).intValue();
            int present = ((Number) data.getOrDefault("presentDays", data.getOrDefault("present", 0))).intValue();
            int late = ((Number) data.getOrDefault("lateDays", data.getOrDefault("late", 0))).intValue();
            int absent = ((Number) data.getOrDefault("absentDays", data.getOrDefault("absent", 0))).intValue();
            double pct = ((Number) data.getOrDefault("attendancePercentage", data.getOrDefault("percentage", 0.0))).doubleValue();
            if (pct == 0.0 && total > 0) {
                pct = Math.round(((double)(present + late) / total) * 1000.0) / 10.0;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("**Your Daily Attendance Summary:**\n\n");
            sb.append("- **Overall Attendance:** **").append(pct).append("%**\n");
            sb.append("- **Present Days:** ").append(present).append("\n");
            sb.append("- **Late Days (Attended):** ").append(late).append("\n");
            sb.append("- **Absent Days:** ").append(absent).append("\n");
            sb.append("- **Total Marked Days:** ").append(total).append("\n\n");
            sb.append("*(Note: Attendance is Daily Morning Attendance taken by your Class Teacher)*");

            return AIResponse.builder().content(sb.toString()).toolUsed("getMyAttendance").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Your daily attendance record: " + json).toolUsed("getMyAttendance").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentTimetable(String json) {
        try {
            List<Map<String, Object>> slots = objectMapper.readValue(json, new TypeReference<>() {});
            if (slots.isEmpty()) {
                return AIResponse.builder().content("No scheduled periods found for your class today.").toolUsed("getMyTimetable").fallbackUsed(true).build();
            }
            StringBuilder sb = new StringBuilder("**Your Class Timetable:**\n\n");
            for (Map<String, Object> s : slots) {
                sb.append("- **Period ").append(s.get("periodNumber")).append("**: ")
                  .append(s.get("subject")).append(" (").append(s.get("dayOfWeek")).append(") — ")
                  .append(s.getOrDefault("startTime", "")).append(" - ").append(s.getOrDefault("endTime", "")).append("\n");
            }
            return AIResponse.builder().content(sb.toString()).toolUsed("getMyTimetable").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Timetable schedule: " + json).toolUsed("getMyTimetable").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentProfile(String json) {
        try {
            Map<String, Object> p = objectMapper.readValue(json, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            sb.append("**Your Student Profile:**\n\n");
            sb.append("- **Name:** ").append(p.get("firstName")).append(" ").append(p.get("lastName")).append("\n");
            sb.append("- **Admission No:** ").append(p.get("admissionNo")).append("\n");
            sb.append("- **Class & Section:** Class ").append(p.get("studentClass")).append("-").append(p.get("section")).append("\n");
            sb.append("- **Roll No:** ").append(p.get("rollNo")).append("\n");
            sb.append("- **Status:** ").append(p.get("status")).append("\n");
            sb.append("- **Father's Name:** ").append(p.get("fatherName")).append("\n");
            sb.append("- **Mother's Name:** ").append(p.get("motherName")).append("\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getMyProfile").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Student Profile: " + json).toolUsed("getMyProfile").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatTeacherSchedule(String json) {
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            if (list.isEmpty()) {
                return AIResponse.builder().content("You have no teaching periods scheduled today.").toolUsed("getTeacherSchedule").fallbackUsed(true).build();
            }
            StringBuilder sb = new StringBuilder("**Your Scheduled Classes:**\n\n");
            for (Map<String, Object> item : list) {
                sb.append("- **Period ").append(item.get("periodNumber")).append("**: Class ")
                  .append(item.get("className")).append("-").append(item.get("section")).append(" (")
                  .append(item.get("subject")).append(") on ").append(item.get("dayOfWeek")).append("\n");
            }
            return AIResponse.builder().content(sb.toString()).toolUsed("getTeacherSchedule").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Teaching Schedule: " + json).toolUsed("getTeacherSchedule").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatTeacherStudents(String json) {
        try {
            List<Map<String, Object>> students = objectMapper.readValue(json, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            sb.append("**Students in Assigned Class (Total: ").append(students.size()).append("):**\n\n");
            for (int i = 0; i < Math.min(15, students.size()); i++) {
                Map<String, Object> s = students.get(i);
                sb.append(i + 1).append(". **").append(s.get("firstName")).append(" ").append(s.get("lastName"))
                  .append("** (Roll: ").append(s.get("rollNo")).append(", Adm: ").append(s.get("admissionNo")).append(")\n");
            }
            if (students.size() > 15) {
                sb.append("... and ").append(students.size() - 15).append(" more students.");
            }
            return AIResponse.builder().content(sb.toString()).toolUsed("getMyStudents").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Class students: " + json).toolUsed("getMyStudents").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatTeacherClasses(String json) {
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json, new TypeReference<>() {});
            if (list.isEmpty()) {
                return AIResponse.builder().content("You currently have no classes assigned.").toolUsed("getMyClasses").fallbackUsed(true).build();
            }
            StringBuilder sb = new StringBuilder("**Your Assigned Teaching Classes:**\n\n");
            for (Map<String, Object> c : list) {
                sb.append("- Class **").append(c.get("className")).append("-").append(c.get("section"))
                  .append("** — Subject: *").append(c.get("subject")).append("*\n");
            }
            return AIResponse.builder().content(sb.toString()).toolUsed("getMyClasses").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Assigned classes: " + json).toolUsed("getMyClasses").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatClassAttendance(String json) {
        try {
            List<Map<String, Object>> logs = objectMapper.readValue(json, new TypeReference<>() {});
            int present = 0, absent = 0, late = 0;
            for (Map<String, Object> l : logs) {
                String st = (String) l.get("status");
                if ("PRESENT".equalsIgnoreCase(st)) present++;
                else if ("LATE".equalsIgnoreCase(st)) late++;
                else if ("ABSENT".equalsIgnoreCase(st)) absent++;
            }
            StringBuilder sb = new StringBuilder("**Class Attendance Telemetry:**\n\n");
            sb.append("- **Total Marked:** ").append(logs.size()).append("\n");
            sb.append("- **Present:** ").append(present).append("\n");
            sb.append("- **Late:** ").append(late).append("\n");
            sb.append("- **Absent:** ").append(absent).append("\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getClassAttendance").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Class attendance: " + json).toolUsed("getClassAttendance").fallbackUsed(true).build();
        }
    }

    private AIResponse formatTeacherProfile(String json) {
        return AIResponse.builder().content("Teacher Profile Details:\n" + json).toolUsed("getMyTeacherProfile").fallbackUsed(true).build();
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatAdminStudents(String json) {
        try {
            Map<String, Object> stats = objectMapper.readValue(json, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            sb.append("**School Student Enrollment Statistics:**\n\n");
            sb.append("- **Total Students:** **").append(stats.get("totalStudents")).append("**\n");
            sb.append("- **Active Students:** ").append(stats.get("activeStudents")).append("\n");
            sb.append("- **Inactive Students:** ").append(stats.get("inactiveStudents")).append("\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getStudentStatistics").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Student statistics: " + json).toolUsed("getStudentStatistics").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatAdminFaculty(String json) {
        try {
            Map<String, Object> stats = objectMapper.readValue(json, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            sb.append("**Faculty Overview:**\n\n");
            sb.append("- **Total Faculty Members:** **").append(stats.get("totalFaculty")).append("**\n");
            sb.append("- **Active Teachers:** ").append(stats.get("activeTeachers")).append("\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getFacultySummary").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Faculty summary: " + json).toolUsed("getFacultySummary").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatAdminAttendance(String json) {
        try {
            Map<String, Object> att = objectMapper.readValue(json, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            sb.append("**School-wide Daily Attendance Analytics:**\n\n");
            sb.append("- **Total Marked Logs:** ").append(att.get("totalMarkedRecords")).append("\n");
            sb.append("- **Present Students:** ").append(att.get("presentCount")).append("\n");
            sb.append("- **Late Students:** ").append(att.get("lateCount")).append("\n");
            sb.append("- **Absent Students:** ").append(att.get("absentCount")).append("\n");
            sb.append("- **Overall Attendance Rate:** **").append(att.get("attendancePercentage")).append("%**\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getAttendanceAnalytics").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Attendance analytics: " + json).toolUsed("getAttendanceAnalytics").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatAdminFees(String json) {
        try {
            Map<String, Object> fee = objectMapper.readValue(json, new TypeReference<>() {});
            double rev = ((Number) fee.getOrDefault("totalRevenue", 0)).doubleValue();
            double pending = ((Number) fee.getOrDefault("pendingAmount", 0)).doubleValue();
            NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

            StringBuilder sb = new StringBuilder();
            sb.append("**School Fee Collection Analytics:**\n\n");
            sb.append("- **Cumulative Revenue Collected:** **").append(inr.format(rev)).append("**\n");
            sb.append("- **Outstanding Pending Fees:** **").append(inr.format(pending)).append("**\n");
            sb.append("- **Fully Cleared Accounts:** ").append(fee.get("paidCount")).append(" students\n");
            sb.append("- **Partially Paid Accounts:** ").append(fee.get("partialCount")).append(" students\n");
            sb.append("- **Unpaid Accounts:** ").append(fee.get("unpaidCount")).append(" students\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getFeeAnalytics").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Fee Analytics: " + json).toolUsed("getFeeAnalytics").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatAdminInquiries(String json) {
        try {
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<>() {});
            StringBuilder sb = new StringBuilder();
            sb.append("**Public Inquiries Status:**\n\n");
            sb.append("- **Total Inquiries Received:** ").append(data.get("totalInquiries")).append("\n");
            sb.append("- **Pending Response:** **").append(data.get("pendingInquiries")).append("**\n");
            return AIResponse.builder().content(sb.toString()).toolUsed("getPendingInquiries").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Inquiries summary: " + json).toolUsed("getPendingInquiries").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatNotices(String json) {
        try {
            List<Map<String, Object>> notices = objectMapper.readValue(json, new TypeReference<>() {});
            if (notices.isEmpty()) {
                return AIResponse.builder().content("No active notices published at this moment.").toolUsed("getLatestNotices").fallbackUsed(true).build();
            }
            StringBuilder sb = new StringBuilder("**Latest Official Notices:**\n\n");
            for (int i = 0; i < Math.min(4, notices.size()); i++) {
                Map<String, Object> n = notices.get(i);
                sb.append("- **").append(n.get("title")).append("** (").append(n.getOrDefault("category", "General")).append(")\n  ")
                  .append(n.getOrDefault("content", "")).append("\n");
            }
            return AIResponse.builder().content(sb.toString()).toolUsed("getLatestNotices").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Notices: " + json).toolUsed("getLatestNotices").fallbackUsed(true).build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatEvents(String json) {
        try {
            List<Map<String, Object>> events = objectMapper.readValue(json, new TypeReference<>() {});
            if (events.isEmpty()) {
                return AIResponse.builder().content("No upcoming events scheduled at this moment.").toolUsed("getUpcomingEvents").fallbackUsed(true).build();
            }
            StringBuilder sb = new StringBuilder("**Upcoming School Events:**\n\n");
            for (int i = 0; i < Math.min(4, events.size()); i++) {
                Map<String, Object> ev = events.get(i);
                sb.append("- **").append(ev.get("title")).append("** — Date: ").append(ev.get("startDate"))
                  .append(" (").append(ev.getOrDefault("location", "Campus")).append(")\n  ")
                  .append(ev.getOrDefault("description", "")).append("\n");
            }
            return AIResponse.builder().content(sb.toString()).toolUsed("getUpcomingEvents").fallbackUsed(true).build();
        } catch (Exception e) {
            return AIResponse.builder().content("Events: " + json).toolUsed("getUpcomingEvents").fallbackUsed(true).build();
        }
    }

    private AIResponse formatGreeting(String role) {
        StringBuilder sb = new StringBuilder();
        if ("STUDENT".equals(role)) {
            sb.append("👋 **Hello! Welcome to Success Academy Assistant.**\n\n");
            sb.append("I am your personalized student portal assistant. Here is what I can help you with:\n\n");
            sb.append("- 📊 **My Attendance** — *\"What is my attendance record?\"*\n");
            sb.append("- 💰 **My Fees & Dues** — *\"Show my fee balance and receipts\"*\n");
            sb.append("- 📅 **My Class Timetable** — *\"What is my schedule for today?\"*\n");
            sb.append("- 📢 **Notices & Events** — *\"Show latest school circulars & upcoming events\"*\n\n");
            sb.append("💡 *Tip: Feel free to type in English, Hindi, or Hinglish — I auto-correct spelling mistakes!*");
        } else if ("TEACHER".equals(role)) {
            sb.append("👋 **Hello Teacher! Welcome to Success Academy Assistant.**\n\n");
            sb.append("Here is what you can quickly check:\n\n");
            sb.append("- 🏫 **My Teaching Schedule** — *\"Show my timetable schedule\"*\n");
            sb.append("- 👥 **Assigned Class Students** — *\"Show students in my class\"*\n");
            sb.append("- 📊 **Daily Attendance Status** — *\"Today's class attendance summary\"*\n");
            sb.append("- 🔍 **Student 360° Lookup** — *\"Details of student Mayank Agarwal\"*\n");
            sb.append("- 📢 **School Circulars** — *\"Show latest notices & upcoming events\"*\n\n");
            sb.append("💡 *Tip: Feel free to type in English, Hindi, or Hinglish — I auto-correct spelling mistakes!*");
        } else { // ADMIN
            sb.append("👋 **Welcome Administrator to Success Academy AI Assistant.**\n\n");
            sb.append("Here are real-time intelligence queries you can run:\n\n");
            sb.append("- 📈 **Enrollment & Statistics** — *\"Total students enrolled & class-wise breakdown\"*\n");
            sb.append("- 💰 **Fee Revenue & Dues** — *\"Total fees collected and outstanding balance\"*\n");
            sb.append("- 📊 **School Attendance Telemetry** — *\"Daily attendance summary of all classes\"*\n");
            sb.append("- 🔍 **Student 360° Profile** — *\"Details of student Mayank Agarwal\"*\n");
            sb.append("- 🔍 **Faculty Profile Lookup** — *\"Information about teacher Priya Mehta\"*\n");
            sb.append("- 👥 **Class Roster** — *\"Students of Class 10-A\"*\n");
            sb.append("- 📩 **Inquiry Tracking** — *\"Show pending website contact inquiries\"*\n\n");
            sb.append("💡 *Tip: Feel free to type in English, Hindi, or Hinglish — I auto-correct spelling mistakes!*");
        }
        return AIResponse.builder().content(sb.toString()).fallbackUsed(true).build();
    }

    private String cleanEntitySearchTerm(String query) {
        String s = query;
        // Strip question / action prefixes
        s = s.replaceAll("(?i)\\b(give\\s+me|can\\s+you\\s+give\\s+me|please\\s+give\\s+me|tell\\s+me|show\\s+me|search\\s+for|find|search|who\\s+is|what\\s+is|where\\s+is)\\b", " ");
        // Strip Hindi/Hinglish question keywords
        s = s.replaceAll("(?i)\\b(batao|dikhao|btao|dkhao|chahiye|pata\\s+karo|jankari|maloomat|kya\\s+hai|kya|hai|hain|ka|ki|ke|ko|se|mein|me|baare|karo)\\b", " ");
        // Strip intent and general descriptor keywords (including common spelling variants)
        s = s.replaceAll("(?i)\\b(atendance|atendence|atandance|attendece|attendance|attendence|attandence|attend|present|absent|late|roll\\s*call|hazri|presenti)\\b", " ");
        s = s.replaceAll("(?i)\\b(fees?|fess|fe|financial|balance|due|dues|payment|receipt|ledger|paisa|rupee|rupees|hisab|shulk)\\b", " ");
        s = s.replaceAll("(?i)\\b(details?|detail|detals|detailes|dtails|information|infomation|info|infor|profile|records?|summary|status|report|names?|list|roster|all)\\b", " ");
        s = s.replaceAll("(?i)\\b(student|students|stuent|studnt|studnet|bachha|baccha|vidyarthi|teacher|teachers|teachr|techr|faculty|staff|prof|professor|sir|madam|miss|mr\\.?|mrs\\.?|shikshak)\\b", " ");
        // Strip prepositions
        s = s.replaceAll("(?i)\\b(about|related\\s+to|regarding|of|for|in|on|from|to|with)\\b", " ");
        // Strip class patterns e.g. "class 10-a", "clas 10-a", "10-a"
        s = s.replaceAll("(?i)\\b(?:class|clas|clss|grade|std|kaksha)\\s*([0-9]{1,2}|nursery|lkg|ukg|[a-zA-Z]{1,3})(?:st|nd|rd|th)?(?:\\s*[-/]?\\s*[a-zA-Z])?\\b", " ");
        s = s.replaceAll("(?i)\\b([0-9]{1,2})(?:st|nd|rd|th)?\\s*[-/]\\s*([a-d])\\b", " ");
        s = s.replaceAll("(?i)\\b(class|clas|clss|grade|std|kaksha|section|secton|sec)\\b", " ");
        // Clean whitespace and punctuation
        s = s.replaceAll("['\"`.,:;?!]", " ").replaceAll("\\s+", " ").trim();
        return s;
    }

    private String extractTeacherSearchTerm(String query) {
        String s = query.replaceAll("(?i)\\bgive me information (?:about|related to|of|regarding)?\\b", "")
                        .replaceAll("(?i)\\bgive me (?:complete )?details (?:of|about|regarding)?\\b", "")
                        .replaceAll("(?i)\\bgive information (?:about|of)?\\b", "")
                        .replaceAll("(?i)\\bgive details (?:of|about)?\\b", "")
                        .replaceAll("(?i)\\bdetails of\\b", "")
                        .replaceAll("(?i)\\binformation (?:about|of|related to)?\\b", "")
                        .replaceAll("(?i)\\btell me about\\b", "")
                        .replaceAll("(?i)\\bwho is\\b", "")
                        .replaceAll("(?i)\\bfind\\b", "")
                        .replaceAll("(?i)\\bsearch\\b", "")
                        .replaceAll("(?i)\\bteacher\\b", "")
                        .replaceAll("(?i)\\bfaculty\\b", "")
                        .replaceAll("(?i)\\bprof(?:essor)?\\b", "")
                        .replaceAll("(?i)\\bsir\\b", "")
                        .replaceAll("(?i)\\bmadam\\b", "")
                        .replaceAll("(?i)\\bmiss\\b", "")
                        .replaceAll("(?i)\\bmr\\.?\\b", "")
                        .replaceAll("(?i)\\bmrs\\.?\\b", "")
                        .trim();
        return s;
    }

    private String extractStudentSearchTerm(String query) {
        String s = query.replaceAll("(?i)\\bgive me (?:complete )?details of student\\b", "")
                        .replaceAll("(?i)\\bgive me (?:complete )?details of\\b", "")
                        .replaceAll("(?i)\\bgive me (?:complete )?detail of student\\b", "")
                        .replaceAll("(?i)\\bgive me (?:complete )?detail of\\b", "")
                        .replaceAll("(?i)\\bgive me information (?:about|related to|of|regarding) student\\b", "")
                        .replaceAll("(?i)\\bgive me information (?:about|related to|of|regarding)?\\b", "")
                        .replaceAll("(?i)\\bgive details of student\\b", "")
                        .replaceAll("(?i)\\bgive information (?:about|of) student\\b", "")
                        .replaceAll("(?i)\\bdetails of student\\b", "")
                        .replaceAll("(?i)\\bdetail of student\\b", "")
                        .replaceAll("(?i)\\binformation about student\\b", "")
                        .replaceAll("(?i)\\binformation of student\\b", "")
                        .replaceAll("(?i)\\btell me about student\\b", "")
                        .replaceAll("(?i)\\bwho is student\\b", "")
                        .replaceAll("(?i)\\bfind student\\b", "")
                        .replaceAll("(?i)\\bsearch student\\b", "")
                        .replaceAll("(?i)\\bdetails of\\b", "")
                        .replaceAll("(?i)\\bdetail of\\b", "")
                        .replaceAll("(?i)\\binformation about\\b", "")
                        .replaceAll("(?i)\\btell me about\\b", "")
                        .replaceAll("(?i)\\bwho is\\b", "")
                        .replaceAll("(?i)\\bfind\\b", "")
                        .replaceAll("(?i)\\bsearch\\b", "")
                        .replaceAll("(?i)\\bstudent\\b", "")
                        .replaceAll("(?i)\\bprofile\\b", "")
                        .replaceAll("(?i)\\binfo\\b", "")
                        .trim();
        s = s.replaceAll("(?i)\\bclass\\s*[0-9]{1,2}(?:st|nd|rd|th)?(?:\\s*[-/]?\\s*[a-d])?\\b", "").trim();
        return s;
    }

    private String extractAttendanceStudentTerm(String query) {
        String s = query.replaceAll("(?i)\\bgive me (?:attendance|attendence) record for\\b", "")
                        .replaceAll("(?i)\\bgive me (?:attendance|attendence) record of\\b", "")
                        .replaceAll("(?i)\\bgive me (?:attendance|attendence) of\\b", "")
                        .replaceAll("(?i)\\bgive me (?:attendance|attendence) for\\b", "")
                        .replaceAll("(?i)\\b(?:attendance|attendence) record for\\b", "")
                        .replaceAll("(?i)\\b(?:attendance|attendence) record of\\b", "")
                        .replaceAll("(?i)\\b(?:attendance|attendence) of\\b", "")
                        .replaceAll("(?i)\\b(?:attendance|attendence) for\\b", "")
                        .replaceAll("(?i)\\b(?:attendance|attendence)\\b", "")
                        .replaceAll("(?i)\\brecord for\\b", "")
                        .replaceAll("(?i)\\brecord of\\b", "")
                        .replaceAll("(?i)\\bstudent\\b", "")
                        .trim();
        s = s.replaceAll("(?i)\\bclass\\s*([0-9]{1,2}|nursery|lkg|ukg|[a-zA-Z]{1,3})(?:st|nd|rd|th)?(?:\\s*[-/]?\\s*[a-zA-Z])?\\b", "").trim();
        return s;
    }

    private String extractFeeStudentTerm(String query) {
        String s = query.replaceAll("(?i)\\bgive me fee (?:details|records|summary) (?:of|for)\\b", "")
                        .replaceAll("(?i)\\bgive me fees (?:of|for)\\b", "")
                        .replaceAll("(?i)\\bfee (?:details|records|summary) (?:of|for)\\b", "")
                        .replaceAll("(?i)\\bfees (?:of|for)\\b", "")
                        .replaceAll("(?i)\\bfee of\\b", "")
                        .replaceAll("(?i)\\bfee for\\b", "")
                        .replaceAll("(?i)\\bfees\\b", "")
                        .replaceAll("(?i)\\bfee\\b", "")
                        .replaceAll("(?i)\\bstudent\\b", "")
                        .trim();
        s = s.replaceAll("(?i)\\bclass\\s*([0-9]{1,2}|nursery|lkg|ukg|[a-zA-Z]{1,3})(?:st|nd|rd|th)?(?:\\s*[-/]?\\s*[a-zA-Z])?\\b", "").trim();
        return s;
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatTeacherSearchResults(String json, String queryTerm) {
        try {
            if (json.contains("\"message\"")) {
                Map<String, Object> msgObj = objectMapper.readValue(json, new TypeReference<>() {});
                return AIResponse.builder()
                    .content((String) msgObj.getOrDefault("message", "No faculty found matching query."))
                    .toolUsed("searchTeacher")
                    .fallbackUsed(true)
                    .build();
            }

            List<Map<String, Object>> teachers = objectMapper.readValue(json, new TypeReference<>() {});
            if (teachers.isEmpty()) {
                return AIResponse.builder()
                    .content("No faculty record found matching '" + queryTerm + "'.")
                    .toolUsed("searchTeacher")
                    .fallbackUsed(true)
                    .build();
            }

            StringBuilder sb = new StringBuilder();
            if (teachers.size() == 1) {
                Map<String, Object> t = teachers.get(0);
                String name = String.valueOf(t.getOrDefault("name", "Faculty Member"));
                sb.append("**Faculty Profile: ").append(name).append("**\n\n");

                sb.append("👨‍🏫 **Professional Details:**\n");
                sb.append("- **Designation:** ").append(t.getOrDefault("designation", "Teacher")).append("\n");
                if (t.get("department") != null) sb.append("- **Department:** ").append(t.get("department")).append("\n");
                if (t.get("subjects") != null) sb.append("- **Subjects Taught:** ").append(t.get("subjects")).append("\n");
                if (t.get("qualification") != null) sb.append("- **Qualification:** ").append(t.get("qualification")).append("\n");
                if (t.get("experience") != null) sb.append("- **Teaching Experience:** ").append(t.get("experience")).append(" years\n");
                sb.append("- **Status:** ").append(t.getOrDefault("status", "Active")).append("\n\n");

                sb.append("🏫 **Class & Academic Assignments:**\n");
                if (t.get("classTeacherOf") != null && !String.valueOf(t.get("classTeacherOf")).isBlank()) {
                    sb.append("- **Designated Class Teacher of:** Class ").append(t.get("classTeacherOf")).append("\n");
                }
                if (t.get("assignedClasses") != null) {
                    sb.append("- **Assigned Classes:** ").append(t.get("assignedClasses")).append("\n");
                }
                sb.append("\n");

                sb.append("📞 **Contact Information:**\n");
                if (t.get("email") != null) sb.append("- **Email:** ").append(t.get("email")).append("\n");
                if (t.get("phone") != null) sb.append("- **Phone:** ").append(t.get("phone")).append("\n");
            } else {
                sb.append("**Found ").append(teachers.size()).append(" matching faculty members:**\n\n");
                for (Map<String, Object> t : teachers) {
                    sb.append("- **").append(t.get("name")).append("** (").append(t.getOrDefault("designation", "Teacher"))
                      .append(" — ").append(t.getOrDefault("department", "General")).append(", Status: ").append(t.getOrDefault("status", "Active")).append(")\n");
                }
            }

            return AIResponse.builder()
                .content(sb.toString())
                .toolUsed("searchTeacher")
                .fallbackUsed(true)
                .build();
        } catch (Exception e) {
            return AIResponse.builder()
                .content("Faculty search results: " + json)
                .toolUsed("searchTeacher")
                .fallbackUsed(true)
                .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentAttendanceRecord(String json, String queryTerm) {
        try {
            if (json.contains("\"message\"")) {
                Map<String, Object> msgObj = objectMapper.readValue(json, new TypeReference<>() {});
                return AIResponse.builder()
                    .content((String) msgObj.getOrDefault("message", "No student found matching query."))
                    .toolUsed("searchStudent")
                    .fallbackUsed(true)
                    .build();
            }

            List<Map<String, Object>> students = objectMapper.readValue(json, new TypeReference<>() {});
            if (students.isEmpty()) {
                return AIResponse.builder()
                    .content("No student attendance record found for '" + queryTerm + "'.")
                    .toolUsed("searchStudent")
                    .fallbackUsed(true)
                    .build();
            }

            Map<String, Object> s = students.get(0);
            String fn = String.valueOf(s.getOrDefault("firstName", ""));
            String ln = String.valueOf(s.getOrDefault("lastName", ""));
            String fullName = (fn + " " + ln).trim();
            String adm = String.valueOf(s.getOrDefault("admissionNo", "-"));
            String cls = s.get("studentClass") + "-" + s.getOrDefault("section", "A");
            String roll = String.valueOf(s.getOrDefault("rollNo", "-"));

            Map<String, Object> att = (Map<String, Object>) s.get("attendanceSummary");
            StringBuilder sb = new StringBuilder();
            sb.append("**Daily Attendance Record: ").append(fullName).append("** (Class ").append(cls).append(" | Roll: ").append(roll).append(")\n\n");

            if (att != null && !att.isEmpty()) {
                int pres = ((Number) att.getOrDefault("presentDays", 0)).intValue();
                int late = ((Number) att.getOrDefault("lateDays", 0)).intValue();
                int abs = ((Number) att.getOrDefault("absentDays", 0)).intValue();
                int total = ((Number) att.getOrDefault("totalDays", 0)).intValue();
                double pct = ((Number) att.getOrDefault("attendancePercentage", 0.0)).doubleValue();
                if (pct == 0.0 && total > 0) {
                    pct = Math.round(((double)(pres + late) / total) * 1000.0) / 10.0;
                }

                sb.append("- **Overall Attendance Rate:** **").append(pct).append("%**\n");
                sb.append("- **Total Marked Days:** ").append(total).append("\n");
                sb.append("- **Present Days:** ").append(pres).append("\n");
                sb.append("- **Late Days (Attended):** ").append(late).append("\n");
                sb.append("- **Absent Days:** ").append(abs).append("\n");
                sb.append("- **Admission No:** `").append(adm).append("`\n\n");
                sb.append("*(Note: Attendance is daily morning roll call taken by Class Teacher)*");
            } else {
                sb.append("- **Status:** Student is enrolled in Class ").append(cls).append(" (Adm: `").append(adm).append("`)\n");
            }

            return AIResponse.builder()
                .content(sb.toString())
                .toolUsed("searchStudent")
                .fallbackUsed(true)
                .build();
        } catch (Exception e) {
            return AIResponse.builder()
                .content("Student attendance record: " + json)
                .toolUsed("searchStudent")
                .fallbackUsed(true)
                .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentFeeRecord(String json, String queryTerm) {
        try {
            if (json.contains("\"message\"")) {
                Map<String, Object> msgObj = objectMapper.readValue(json, new TypeReference<>() {});
                return AIResponse.builder()
                    .content((String) msgObj.getOrDefault("message", "No student found matching query."))
                    .toolUsed("searchStudent")
                    .fallbackUsed(true)
                    .build();
            }

            List<Map<String, Object>> students = objectMapper.readValue(json, new TypeReference<>() {});
            if (students.isEmpty()) {
                return AIResponse.builder()
                    .content("No student fee record found for '" + queryTerm + "'.")
                    .toolUsed("searchStudent")
                    .fallbackUsed(true)
                    .build();
            }

            Map<String, Object> s = students.get(0);
            String fn = String.valueOf(s.getOrDefault("firstName", ""));
            String ln = String.valueOf(s.getOrDefault("lastName", ""));
            String fullName = (fn + " " + ln).trim();
            String adm = String.valueOf(s.getOrDefault("admissionNo", "-"));
            String cls = s.get("studentClass") + "-" + s.getOrDefault("section", "A");

            List<Map<String, Object>> fees = (List<Map<String, Object>>) s.get("feeRecords");
            StringBuilder sb = new StringBuilder();
            sb.append("**Fee Ledger: ").append(fullName).append("** (Class ").append(cls).append(" | Adm: `").append(adm).append("`)\n\n");

            if (fees != null && !fees.isEmpty()) {
                double totAmt = 0;
                double totPaid = 0;
                String feeStatus = "Cleared";
                String dueDate = "";
                for (Map<String, Object> fr : fees) {
                    totAmt += ((Number) fr.getOrDefault("amount", 0)).doubleValue();
                    totPaid += ((Number) fr.getOrDefault("paidAmount", 0)).doubleValue();
                    feeStatus = String.valueOf(fr.getOrDefault("status", feeStatus));
                    dueDate = String.valueOf(fr.getOrDefault("dueDate", dueDate));
                }
                double bal = Math.max(0, totAmt - totPaid);
                NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

                sb.append("- **Total Fees Assigned:** ").append(inr.format(totAmt)).append("\n");
                sb.append("- **Amount Paid:** ").append(inr.format(totPaid)).append("\n");
                sb.append("- **Remaining Balance Due:** **").append(inr.format(bal)).append("**\n");
                sb.append("- **Payment Status:** ").append(feeStatus).append("\n");
                if (!dueDate.isBlank()) {
                    sb.append("- **Due Date:** ").append(dueDate).append("\n");
                }
            } else {
                sb.append("- **Status:** No fee records found.\n");
            }

            return AIResponse.builder()
                .content(sb.toString())
                .toolUsed("searchStudent")
                .fallbackUsed(true)
                .build();
        } catch (Exception e) {
            return AIResponse.builder()
                .content("Student fee record: " + json)
                .toolUsed("searchStudent")
                .fallbackUsed(true)
                .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatStudentSearchResults(String json, String queryTerm) {
        try {
            if (json.contains("\"message\"")) {
                Map<String, Object> msgObj = objectMapper.readValue(json, new TypeReference<>() {});
                return AIResponse.builder()
                    .content((String) msgObj.getOrDefault("message", "No student found matching query."))
                    .toolUsed("searchStudent")
                    .fallbackUsed(true)
                    .build();
            }

            List<Map<String, Object>> students = objectMapper.readValue(json, new TypeReference<>() {});
            if (students.isEmpty()) {
                return AIResponse.builder()
                    .content("No student record found matching '" + queryTerm + "'.")
                    .toolUsed("searchStudent")
                    .fallbackUsed(true)
                    .build();
            }

            StringBuilder sb = new StringBuilder();
            if (students.size() == 1 || (queryTerm.trim().contains(" ") && students.size() <= 5)) {
                Map<String, Object> s = students.get(0);
                String fn = String.valueOf(s.getOrDefault("firstName", ""));
                String ln = String.valueOf(s.getOrDefault("lastName", ""));
                String fullName = (fn + " " + ln).trim();

                sb.append("**Complete Student Profile: ").append(fullName).append("**\n\n");

                sb.append("🎓 **Academic Details:**\n");
                sb.append("- **Admission No:** `").append(s.get("admissionNo")).append("`\n");
                sb.append("- **Class & Section:** Class ").append(s.get("studentClass")).append("-").append(s.getOrDefault("section", "A")).append("\n");
                sb.append("- **Roll No:** ").append(s.get("rollNo")).append("\n");
                sb.append("- **Status:** ").append(s.getOrDefault("status", "Active")).append("\n\n");

                // Attendance Section
                Map<String, Object> att = (Map<String, Object>) s.get("attendanceSummary");
                sb.append("📊 **Attendance Overview:**\n");
                if (att != null && !att.isEmpty()) {
                    int pres = ((Number) att.getOrDefault("presentDays", 0)).intValue();
                    int late = ((Number) att.getOrDefault("lateDays", 0)).intValue();
                    int abs = ((Number) att.getOrDefault("absentDays", 0)).intValue();
                    int total = ((Number) att.getOrDefault("totalDays", 0)).intValue();
                    double pct = ((Number) att.getOrDefault("attendancePercentage", 0.0)).doubleValue();
                    if (pct == 0.0 && total > 0) {
                        pct = Math.round(((double)(pres + late) / total) * 1000.0) / 10.0;
                    }
                    sb.append("- **Attendance Rate:** **").append(pct).append("%**\n");
                    sb.append("- **Marked Days:** ").append(total).append(" (Present: ").append(pres).append(", Late: ").append(late).append(", Absent: ").append(abs).append(")\n\n");
                } else {
                    sb.append("- **Attendance Rate:** Recorded in live ledger\n\n");
                }

                // Fees Section
                List<Map<String, Object>> fees = (List<Map<String, Object>>) s.get("feeRecords");
                sb.append("💰 **Fee & Financial Status:**\n");
                if (fees != null && !fees.isEmpty()) {
                    double totAmt = 0;
                    double totPaid = 0;
                    String feeStatus = "Cleared";
                    for (Map<String, Object> fr : fees) {
                        totAmt += ((Number) fr.getOrDefault("amount", 0)).doubleValue();
                        totPaid += ((Number) fr.getOrDefault("paidAmount", 0)).doubleValue();
                        feeStatus = String.valueOf(fr.getOrDefault("status", feeStatus));
                    }
                    double bal = Math.max(0, totAmt - totPaid);
                    NumberFormat inr = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                    sb.append("- **Total Fees:** ").append(inr.format(totAmt)).append("\n");
                    sb.append("- **Amount Paid:** ").append(inr.format(totPaid)).append("\n");
                    sb.append("- **Outstanding Balance:** ").append(inr.format(bal)).append(" (").append(feeStatus).append(")\n\n");
                } else {
                    sb.append("- **Fee Status:** Regular Ledger\n\n");
                }

                sb.append("👨‍👩‍👧 **Parent & Contact Information:**\n");
                if (s.get("fatherName") != null) sb.append("- **Father's Name:** ").append(s.get("fatherName")).append("\n");
                if (s.get("motherName") != null) sb.append("- **Mother's Name:** ").append(s.get("motherName")).append("\n");
                if (s.get("email") != null) sb.append("- **Email:** ").append(s.get("email")).append("\n");
                if (s.get("phone") != null) sb.append("- **Phone:** ").append(s.get("phone")).append("\n");
                if (s.get("city") != null) sb.append("- **City / Address:** ").append(s.get("city")).append(", ").append(s.getOrDefault("state", "MP")).append("\n");

                if (students.size() > 1) {
                    sb.append("\n*(Other potential matches: ");
                    for (int i = 1; i < students.size(); i++) {
                        Map<String, Object> os = students.get(i);
                        sb.append(os.get("firstName")).append(" ").append(os.get("lastName")).append(" (").append(os.get("admissionNo")).append(")");
                        if (i < students.size() - 1) sb.append(", ");
                    }
                    sb.append(")*\n");
                }
            } else {
                sb.append("**Found ").append(students.size()).append(" matching students:**\n\n");
                for (Map<String, Object> s : students) {
                    sb.append("- **").append(s.get("firstName")).append(" ").append(s.get("lastName")).append("** (Adm: ")
                      .append(s.get("admissionNo")).append(", Class: ").append(s.get("studentClass")).append("-").append(s.get("section"))
                      .append(", Status: ").append(s.get("status")).append(")\n");
                }
            }

            return AIResponse.builder()
                .content(sb.toString())
                .toolUsed("searchStudent")
                .fallbackUsed(true)
                .build();
        } catch (Exception e) {
            return AIResponse.builder()
                .content("Student search results: " + json)
                .toolUsed("searchStudent")
                .fallbackUsed(true)
                .build();
        }
    }

    @SuppressWarnings("unchecked")
    private AIResponse formatClassStudentsList(String json, String className, String section) {
        try {
            List<Map<String, Object>> students = objectMapper.readValue(json, new TypeReference<>() {});
            if (students.isEmpty()) {
                String secText = (section != null && !section.isBlank()) ? "-" + section.toUpperCase() : "";
                return AIResponse.builder()
                    .content("No students found in Class " + className.toUpperCase() + secText + ".")
                    .toolUsed("getClassStudents")
                    .fallbackUsed(true)
                    .build();
            }

            StringBuilder sb = new StringBuilder();
            String secTitle = (section != null && !section.isBlank()) ? "-" + section.toUpperCase() : "";
            sb.append("**Students in Class ").append(className.toUpperCase()).append(secTitle)
              .append(" (Total: ").append(students.size()).append("):**\n\n");

            for (int i = 0; i < students.size(); i++) {
                Map<String, Object> s = students.get(i);
                String fn = (String) s.getOrDefault("firstName", "");
                String ln = (String) s.getOrDefault("lastName", "");
                String name = (fn + " " + ln).trim();
                String roll = String.valueOf(s.getOrDefault("rollNo", "-"));
                String adm = String.valueOf(s.getOrDefault("admissionNo", "-"));
                String status = String.valueOf(s.getOrDefault("status", "Active"));

                sb.append(i + 1).append(". **").append(name).append("** — Roll: ").append(roll)
                  .append(" | Adm: `").append(adm).append("`");
                if (!"Active".equalsIgnoreCase(status)) {
                    sb.append(" (").append(status).append(")");
                }
                sb.append("\n");
            }

            return AIResponse.builder()
                .content(sb.toString())
                .toolUsed("getClassStudents")
                .fallbackUsed(true)
                .build();
        } catch (Exception e) {
            return AIResponse.builder()
                .content("Class student records: " + json)
                .toolUsed("getClassStudents")
                .fallbackUsed(true)
                .build();
        }
    }

    private static class ClassSectionInfo {
        String studentClass;
        String section;
    }

    private ClassSectionInfo extractClassAndSection(String query) {
        String q = query.toLowerCase();

        // Pattern 1: e.g. "class 10-a", "clas 10-a", "clss 10-a", "class 10 a", "class 10 section a", "class 10th a", "class 10", "class 9-b"
        Pattern p1 = Pattern.compile("\\b(?:class|clas|clss|grade|std|kaksha)\\s*([0-9]{1,2}|nursery|lkg|ukg)(?:st|nd|rd|th)?(?:\\s*(?:section|secton|sec|-|/)?\\s*([a-d]))?\\b");
        Matcher m1 = p1.matcher(q);
        if (m1.find()) {
            ClassSectionInfo info = new ClassSectionInfo();
            info.studentClass = m1.group(1);
            info.section = m1.group(2) != null ? m1.group(2).toUpperCase() : null;
            return info;
        }

        // Pattern 2: e.g. "10-a", "10a", "9-b", "10th a", "10th-a", "12-c"
        Pattern p2 = Pattern.compile("\\b([0-9]{1,2})(?:st|nd|rd|th)?\\s*[-/]\\s*([a-d])\\b");
        Matcher m2 = p2.matcher(q);
        if (m2.find()) {
            ClassSectionInfo info = new ClassSectionInfo();
            info.studentClass = m2.group(1);
            info.section = m2.group(2).toUpperCase();
            return info;
        }

        return null;
    }
}
