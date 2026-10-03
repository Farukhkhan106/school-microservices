package com.successacademy.chatbotservice.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.chatbotservice.dto.ChatActionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnswerComposer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Locale INDIA_LOCALE = new Locale("en", "IN");

    public AIResponse compose(DataJoinerAndAggregator.JoinedExecutionResult result,
                              String role,
                              String originalQuery) {

        // 1. Check for Write Attempts
        if (result.isWriteAttempt()) {
            return AIResponse.builder()
                .content(result.getWriteAttemptMessage())
                .toolUsed("WriteOperationGuard")
                .toolsInvoked(Collections.singletonList("WriteOperationGuard"))
                .actions(Collections.emptyList())
                .fallbackUsed(true)
                .build();
        }

        // 2. Check for Authorization Denials
        if (result.getDenials() != null && !result.getDenials().isEmpty()) {
            String denialMsg = String.join("\n\n", result.getDenials());
            return AIResponse.builder()
                .content(denialMsg)
                .toolUsed("AuthorizationGuard")
                .toolsInvoked(Collections.singletonList("AuthorizationGuard"))
                .actions(Collections.emptyList())
                .fallbackUsed(true)
                .build();
        }

        // 3. Handle Empty or Unknown Questions
        if (result.getToolOutputs() == null || result.getToolOutputs().isEmpty()) {
            return formatHelpfulUnknownResponse(role, originalQuery);
        }

        StringBuilder sb = new StringBuilder();
        List<ChatActionDto> actions = new ArrayList<>();
        Map<String, Object> outputs = result.getToolOutputs();

        // ── STUDENT OVERVIEW / PROFILE ──
        if (outputs.containsKey("getMyProfile")) {
            appendStudentProfile(sb, outputs.get("getMyProfile"));
        }

        // ── ATTENDANCE ──
        if (outputs.containsKey("getMyAttendance")) {
            appendStudentAttendance(sb, outputs.get("getMyAttendance"), actions, role);
        }
        if (outputs.containsKey("getClassAttendance")) {
            appendClassAttendance(sb, outputs.get("getClassAttendance"), result.getExtractedClass(), result.getExtractedSection(), actions, role);
        }
        if (outputs.containsKey("getAttendanceAnalytics")) {
            appendAttendanceAnalytics(sb, outputs.get("getAttendanceAnalytics"), actions);
        }

        // ── FEES ──
        if (outputs.containsKey("getMyFees")) {
            appendStudentFees(sb, outputs.get("getMyFees"), actions);
        }
        if (outputs.containsKey("getFeeAnalytics")) {
            appendFeeAnalytics(sb, outputs.get("getFeeAnalytics"), actions);
        }
        if (outputs.containsKey("getFeeStructures")) {
            appendFeeStructures(sb, outputs.get("getFeeStructures"), actions);
        }

        // ── TIMETABLE & SCHEDULE ──
        if (outputs.containsKey("getMyTimetable")) {
            appendStudentTimetable(sb, outputs.get("getMyTimetable"), actions);
        }
        if (outputs.containsKey("getTeacherSchedule")) {
            appendTeacherSchedule(sb, outputs.get("getTeacherSchedule"), actions);
        }
        if (outputs.containsKey("getClassTimetable")) {
            appendClassTimetable(sb, outputs.get("getClassTimetable"), result.getExtractedClass(), result.getExtractedSection());
        }

        // ── STUDENTS / ROSTER ──
        if (outputs.containsKey("getClassStudents")) {
            appendClassStudents(sb, outputs.get("getClassStudents"), result.getExtractedClass(), result.getExtractedSection());
        }
        if (outputs.containsKey("getStudentStatistics")) {
            appendStudentStatistics(sb, outputs.get("getStudentStatistics"), actions);
        }
        if (outputs.containsKey("searchStudent")) {
            appendStudentSearchResults(sb, outputs.get("searchStudent"), actions);
        }

        // ── TEACHERS & FACULTY ──
        if (outputs.containsKey("getMyTeacherProfile")) {
            appendTeacherProfile(sb, outputs.get("getMyTeacherProfile"));
        }
        if (outputs.containsKey("getMyClasses")) {
            appendTeacherClasses(sb, outputs.get("getMyClasses"));
        }
        if (outputs.containsKey("searchTeacher")) {
            appendTeacherSearch(sb, outputs.get("searchTeacher"));
        }
        if (outputs.containsKey("getFacultySummary")) {
            appendFacultySummary(sb, outputs.get("getFacultySummary"), actions);
        }

        // ── NOTICES & EVENTS ──
        if (outputs.containsKey("getLatestNotices")) {
            appendNotices(sb, outputs.get("getLatestNotices"), actions);
        }
        if (outputs.containsKey("getUpcomingEvents")) {
            appendEvents(sb, outputs.get("getUpcomingEvents"), actions);
        }

        // ── FACILITIES & CALENDAR ──
        if (outputs.containsKey("getSchoolFacilities")) {
            appendSchoolFacilities(sb, outputs.get("getSchoolFacilities"), result.getFacilityQuery());
        }
        if (outputs.containsKey("getAcademicCalendar")) {
            appendAcademicCalendar(sb, outputs.get("getAcademicCalendar"), actions);
        }
        if (outputs.containsKey("getPendingInquiries")) {
            appendPendingInquiries(sb, outputs.get("getPendingInquiries"), actions);
        }

        String finalContent = sb.toString().trim();
        if (finalContent.isEmpty()) {
            finalContent = "I have checked the school system, but could not find specific records matching your inquiry.";
        }

        String primaryTool = result.getToolsInvoked().isEmpty() ? "CompositeAggregator" : String.join(", ", result.getToolsInvoked());

        return AIResponse.builder()
            .content(finalContent)
            .toolUsed(primaryTool)
            .toolsInvoked(result.getToolsInvoked())
            .actions(actions)
            .fallbackUsed(true)
            .build();
    }

    // ── FORMATTERS ──

    private void appendStudentProfile(StringBuilder sb, Object data) {
        if (!(data instanceof Map)) return;
        Map<String, Object> p = (Map<String, Object>) data;
        sb.append("### 👤 Student Profile\n");
        sb.append("• **Name:** ").append(p.getOrDefault("name", "Student")).append("\n");
        sb.append("• **Admission No:** ").append(p.getOrDefault("admissionNo", "N/A")).append("\n");
        sb.append("• **Class:** ").append(p.getOrDefault("studentClass", "")).append("-").append(p.getOrDefault("section", "")).append("\n");
        if (p.get("rollNumber") != null) sb.append("• **Roll No:** ").append(p.get("rollNumber")).append("\n");
        sb.append("\n");
    }

    private void appendStudentAttendance(StringBuilder sb, Object data, List<ChatActionDto> actions, String role) {
        if (!(data instanceof Map)) return;
        Map<String, Object> a = (Map<String, Object>) data;
        sb.append("### 📋 Daily Attendance Status\n");
        Object pct = a.get("attendancePercentage");
        if (pct != null) {
            sb.append("• **Overall Attendance Rate:** **").append(pct).append("%**\n");
        }
        if (a.get("presentDays") != null) sb.append("• **Days Present:** ").append(a.get("presentDays")).append("\n");
        if (a.get("absentDays") != null) sb.append("• **Days Absent:** ").append(a.get("absentDays")).append("\n");
        sb.append("• *Note: Attendance is tracked as full-day school attendance.*\n\n");

        actions.add(ChatActionDto.builder()
            .label("📊 View Detailed Attendance")
            .url("/student/attendance")
            .build());
    }

    private void appendClassAttendance(StringBuilder sb, Object data, String className, String section, List<ChatActionDto> actions, String role) {
        sb.append("### 📋 Class ").append(className != null ? className : "").append("-").append(section != null ? section : "").append(" Attendance\n");
        if (data instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) data;
            if (m.get("presentCount") != null) sb.append("• **Present:** ").append(m.get("presentCount")).append(" students\n");
            if (m.get("absentCount") != null) sb.append("• **Absent:** ").append(m.get("absentCount")).append(" students\n");
        } else if (data instanceof List) {
            List list = (List) data;
            sb.append("• **Attendance Records Recorded:** ").append(list.size()).append("\n");
        }
        sb.append("\n");

        if ("TEACHER".equals(role)) {
            actions.add(ChatActionDto.builder()
                .label("✍️ Mark Attendance")
                .url("/teacher/attendance")
                .primary(true)
                .build());
        }
    }

    private void appendAttendanceAnalytics(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        if (!(data instanceof Map)) return;
        Map<String, Object> a = (Map<String, Object>) data;
        sb.append("### 📊 School-Wide Daily Attendance Telemetry\n");
        Object rate = a.get("attendancePercentage") != null ? a.get("attendancePercentage") : a.get("overallRate");
        if (rate != null) sb.append("• **Average Attendance Rate:** **").append(rate).append("%**\n");
        Object present = a.get("presentCount") != null ? a.get("presentCount") : a.get("totalPresent");
        if (present != null) sb.append("• **Total Present:** ").append(present).append(" students\n");
        Object absent = a.get("absentCount") != null ? a.get("absentCount") : a.get("totalAbsent");
        if (absent != null) sb.append("• **Total Absent:** ").append(absent).append(" students\n");
        if (a.get("lateCount") != null) sb.append("• **Late Arrivals:** ").append(a.get("lateCount")).append(" students\n");
        sb.append("• *Note: Attendance is tracked as daily full-day school attendance.*\n\n");

        actions.add(ChatActionDto.builder()
            .label("📈 Open Attendance Analytics")
            .url("/admin/attendance")
            .build());
    }

    private void appendStudentFees(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 💳 Fee Account Summary\n");
        long outstanding = 0;
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            for (Map<String, Object> r : list) {
                long amount = toLong(r.get("amount"));
                long paid = toLong(r.get("paidAmount"));
                long bal = amount - paid;
                if (bal > 0) outstanding += bal;
                sb.append("• **").append(r.getOrDefault("feeType", "Tuition")).append(":** ")
                  .append("Total ").append(formatINR(amount))
                  .append(" (Paid: ").append(formatINR(paid)).append(", Pending: **").append(formatINR(bal)).append("**)\n");
            }
        }
        sb.append("• **Net Outstanding Balance:** **").append(formatINR(outstanding)).append("**\n\n");

        if (outstanding > 0) {
            actions.add(ChatActionDto.builder()
                .label("💳 Pay Fee (" + formatINR(outstanding) + ")")
                .url("/student/fees")
                .primary(true)
                .build());
        }
    }

    private void appendFeeAnalytics(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        if (!(data instanceof Map)) return;
        Map<String, Object> f = (Map<String, Object>) data;
        sb.append("### 💰 School Fee Collections Telemetry\n");
        long totalCol = toLong(f.get("totalRevenue"));
        long pending = toLong(f.get("pendingFees"));
        sb.append("• **Total Revenue Collected:** **").append(formatINR(totalCol)).append("**\n");
        sb.append("• **Total Outstanding Fees:** **").append(formatINR(pending)).append("**\n\n");

        actions.add(ChatActionDto.builder()
            .label("📊 View Fee Ledger")
            .url("/admin/fees")
            .build());
    }

    private void appendFeeStructures(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 📋 School Fee Schedule\n");
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            for (Map<String, Object> s : list) {
                sb.append("• **").append(s.getOrDefault("className", "Standard")).append(":** ")
                  .append(formatINR(toLong(s.get("totalFee")))).append(" annual\n");
            }
        }
        sb.append("\n");
    }

    private void appendStudentTimetable(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 📅 Class Timetable & Schedule\n");
        if (data instanceof List) {
            List<Map<String, Object>> periods = (List<Map<String, Object>>) data;
            if (periods.isEmpty()) {
                sb.append("• No periods scheduled for this time.\n");
            } else {
                for (Map<String, Object> p : periods) {
                    sb.append("• **Period ").append(p.getOrDefault("periodNo", "-")).append(":** ")
                      .append(p.getOrDefault("subject", "Subject")).append(" (")
                      .append(p.getOrDefault("teacherName", "Faculty")).append(") - ")
                      .append(p.getOrDefault("startTime", "")).append(" to ").append(p.getOrDefault("endTime", "")).append("\n");
                }
            }
        }
        sb.append("\n");

        actions.add(ChatActionDto.builder()
            .label("📅 Full Timetable")
            .url("/student/academics")
            .build());
    }

    private void appendTeacherSchedule(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### ⏰ Teacher Daily Teaching Schedule\n");
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            for (Map<String, Object> p : list) {
                sb.append("• **Period ").append(p.getOrDefault("periodNo", "-")).append(":** Class ")
                  .append(p.getOrDefault("studentClass", "")).append("-").append(p.getOrDefault("section", ""))
                  .append(" (").append(p.getOrDefault("subject", "")).append(") [")
                  .append(p.getOrDefault("startTime", "")).append(" - ").append(p.getOrDefault("endTime", "")).append("]\n");
            }
        }
        sb.append("\n");

        actions.add(ChatActionDto.builder()
            .label("⏰ View My Timetable")
            .url("/teacher/timetable")
            .build());
    }

    private void appendClassTimetable(StringBuilder sb, Object data, String className, String section) {
        sb.append("### 📅 Timetable for Class ").append(className).append("-").append(section).append("\n");
        if (data instanceof List) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) data;
            for (Map<String, Object> p : list) {
                sb.append("• **Period ").append(p.getOrDefault("periodNo", "-")).append(":** ")
                  .append(p.getOrDefault("subject", "")).append(" by ").append(p.getOrDefault("teacherName", "Faculty"))
                  .append(" (").append(p.getOrDefault("startTime", "")).append(" - ").append(p.getOrDefault("endTime", "")).append(")\n");
            }
        }
        sb.append("\n");
    }

    private void appendClassStudents(StringBuilder sb, Object data, String className, String section) {
        sb.append("### 👨‍🎓 Enrolled Students in Class ").append(className).append("-").append(section).append("\n");
        if (data instanceof List) {
            List<Map<String, Object>> students = (List<Map<String, Object>>) data;
            sb.append("• **Total Enrolled:** **").append(students.size()).append(" students**\n");
            int limit = Math.min(5, students.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> s = students.get(i);
                sb.append("  ").append(i + 1).append(". ").append(resolveStudentName(s))
                  .append(" (Roll No: ").append(s.getOrDefault("rollNumber", "N/A")).append(")\n");
            }
            if (students.size() > 5) {
                sb.append("  *...and ").append(students.size() - 5).append(" more students.*\n");
            }
        }
        sb.append("\n");
    }

    private void appendStudentStatistics(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        if (!(data instanceof Map)) return;
        Map<String, Object> s = (Map<String, Object>) data;
        Object total = s.getOrDefault("totalStudents", 0);
        Object active = s.getOrDefault("activeStudents", 0);
        Object inactive = s.getOrDefault("inactiveStudents", 0);

        sb.append("### 📈 Student Enrollment & Demographics\n");
        sb.append("• **Total Students Enrolled:** **").append(total).append(" students**\n");
        sb.append("• **Active Status:** **").append(active).append(" Active**");
        if (inactive != null && !"0".equals(String.valueOf(inactive))) {
            sb.append(" (").append(inactive).append(" Inactive/Alumni)");
        }
        sb.append("\n");

        Object perClassObj = s.get("studentsPerClass");
        if (perClassObj instanceof Map) {
            Map<?, ?> perClass = (Map<?, ?>) perClassObj;
            if (!perClass.isEmpty()) {
                sb.append("• **Class Distribution:** ");
                List<String> classParts = new ArrayList<>();
                for (Map.Entry<?, ?> entry : perClass.entrySet()) {
                    classParts.add("Class " + entry.getKey() + ": **" + entry.getValue() + "**");
                }
                sb.append(String.join(", ", classParts)).append("\n");
            }
        }
        sb.append("\n");

        actions.add(ChatActionDto.builder()
            .label("👥 View Student Directory")
            .url("/admin/students")
            .build());
        actions.add(ChatActionDto.builder()
            .label("➕ New Admission")
            .url("/admin/students/new")
            .build());
    }

    private void appendStudentSearchResults(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 🔍 Student Search Results\n");
        if (data instanceof List) {
            List<Map<String, Object>> results = (List<Map<String, Object>>) data;
            if (results.isEmpty()) {
                sb.append("No matching student records found. Please check spelling or verify the admission number.\n");
            } else {
                for (Map<String, Object> s : results) {
                    sb.append("• **").append(resolveStudentName(s)).append("**\n");
                    sb.append("  - **Admission No:** ").append(s.getOrDefault("admissionNo", "N/A"));
                    sb.append(" | **Class:** ").append(s.getOrDefault("studentClass", "-")).append("-").append(s.getOrDefault("section", "-"));
                    if (s.containsKey("rollNumber") && s.get("rollNumber") != null) {
                        sb.append(" | **Roll No:** ").append(s.get("rollNumber"));
                    }
                    if (s.containsKey("status") && s.get("status") != null) {
                        sb.append(" | **Status:** ").append(s.get("status"));
                    }
                    sb.append("\n");
                    if (s.containsKey("guardianName") && s.get("guardianName") != null) {
                        sb.append("  - **Guardian:** ").append(s.get("guardianName"));
                    }
                    if (s.containsKey("contactNumber") && s.get("contactNumber") != null) {
                        sb.append(" | **Contact:** ").append(s.get("contactNumber"));
                    }
                    if (s.containsKey("attendancePercentage")) {
                        sb.append("\n  - **Attendance:** **").append(s.get("attendancePercentage")).append("%**");
                    }
                    if (s.containsKey("pendingFee")) {
                        sb.append(" | **Fee Due:** ₹").append(s.get("pendingFee"));
                    }
                    sb.append("\n");
                }
                actions.add(ChatActionDto.builder()
                    .label("👥 View Student Directory")
                    .url("/admin/students")
                    .build());
            }
        } else if (data instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) data;
            if (m.containsKey("message")) {
                sb.append(m.get("message")).append("\n");
            }
        }
        sb.append("\n");
    }

    private String resolveStudentName(Map<String, Object> s) {
        if (s == null) return "Student";
        String name = (String) s.get("name");
        if (name == null || name.isBlank()) {
            String fn = String.valueOf(s.getOrDefault("firstName", "")).trim();
            String ln = String.valueOf(s.getOrDefault("lastName", "")).trim();
            name = (fn + " " + ln).trim();
        }
        return name.isBlank() ? "Student" : name;
    }

    private void appendTeacherProfile(StringBuilder sb, Object data) {
        if (!(data instanceof Map)) return;
        Map<String, Object> t = (Map<String, Object>) data;
        sb.append("### 👨‍🏫 Teacher Profile\n");
        sb.append("• **Name:** ").append(t.getOrDefault("name", "Faculty")).append("\n");
        sb.append("• **Designation:** ").append(t.getOrDefault("designation", "Educator")).append("\n");
        sb.append("• **Qualification:** ").append(t.getOrDefault("qualification", "B.Ed.")).append("\n\n");
    }

    private void appendTeacherClasses(StringBuilder sb, Object data) {
        sb.append("### 📚 Assigned Classes & Subjects\n");
        if (data instanceof List) {
            List list = (List) data;
            for (Object item : list) {
                sb.append("• ").append(item).append("\n");
            }
        }
        sb.append("\n");
    }

    private void appendTeacherSearch(StringBuilder sb, Object data) {
        sb.append("### 👨‍🏫 Faculty Directory Information\n");
        if (data instanceof List) {
            List<Map<String, Object>> teachers = (List<Map<String, Object>>) data;
            for (Map<String, Object> t : teachers) {
                sb.append("• **").append(t.getOrDefault("name", "Faculty")).append("** - ")
                  .append(t.getOrDefault("designation", "Educator")).append(" (")
                  .append(t.getOrDefault("qualification", "")).append(")\n");
            }
        }
        sb.append("\n");
    }

    private void appendFacultySummary(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        if (!(data instanceof Map)) return;
        Map<String, Object> f = (Map<String, Object>) data;
        Object total = f.containsKey("totalFaculty") ? f.get("totalFaculty") : f.getOrDefault("totalTeachers", "0");
        sb.append("### 👨‍🏫 Faculty Overview\n");
        sb.append("• **Total Faculty Members:** **").append(total).append(" Teachers**\n");
        if (f.containsKey("activeTeachers")) {
            sb.append("• **Active Status:** **").append(f.get("activeTeachers")).append(" Active**\n");
        }
        if (f.containsKey("departments") && f.get("departments") != null) {
            sb.append("• **Departments:** ").append(f.get("departments")).append("\n");
        }
        sb.append("\n");

        actions.add(ChatActionDto.builder()
            .label("👥 Faculty Directory")
            .url("/admin/faculty")
            .build());
    }

    private void appendNotices(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 📢 Official Announcements & Circulars\n");
        if (data instanceof List) {
            List<Map<String, Object>> notices = (List<Map<String, Object>>) data;
            int limit = Math.min(3, notices.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> n = notices.get(i);
                sb.append("• **").append(n.getOrDefault("title", "Notice")).append("** (")
                  .append(n.getOrDefault("publishedAt", "")).append("): ")
                  .append(n.getOrDefault("content", "")).append("\n");
            }
        }
        sb.append("\n");

        actions.add(ChatActionDto.builder()
            .label("📢 Notice Board")
            .url("/notices")
            .build());
    }

    private void appendEvents(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 🏆 Upcoming Events & Celebrations\n");
        if (data instanceof List) {
            List<Map<String, Object>> events = (List<Map<String, Object>>) data;
            int limit = Math.min(3, events.size());
            for (int i = 0; i < limit; i++) {
                Map<String, Object> e = events.get(i);
                sb.append("• **").append(e.getOrDefault("title", "Event")).append(":** ")
                  .append(e.getOrDefault("eventDate", "")).append(" at ")
                  .append(e.getOrDefault("location", "School Campus")).append("\n");
            }
        }
        sb.append("\n");

        actions.add(ChatActionDto.builder()
            .label("🏆 School Events")
            .url("/events")
            .build());
    }

    private void appendSchoolFacilities(StringBuilder sb, Object data, String keyword) {
        sb.append("### 🏫 Campus Infrastructure & Facilities\n");
        sb.append("• **Modern Computer Laboratory:** 50 high-performance workstations, gigabit fiber internet, robotics experimentation kits, and programming software suites.\n");
        sb.append("• **Science Laboratories:** Dedicated Physics, Chemistry, and Biology laboratories equipped with modern instruments and safety apparatus.\n");
        sb.append("• **Central Library:** Over 10,000+ reference volumes, encyclopedias, journals, and dedicated reading zones.\n");
        sb.append("• **Sports Complex:** Olympic running track, cricket practice nets, football pitch, indoor badminton court, and basketball facility.\n");
        sb.append("• **School Bus Fleet:** GPS-monitored school buses covering Satwas town and neighboring routes.\n\n");
    }

    private void appendAcademicCalendar(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### 🗓️ Academic Calendar & Important Dates\n");
        sb.append("• **Academic Session:** 2024–2025 (CBSE English Medium)\n");
        sb.append("• **Mid-Term Examinations:** Commencing January 15\n");
        sb.append("• **Winter Vacation:** December 25 to January 5\n");
        sb.append("• **Annual Examinations:** Commencing March 10\n\n");

        actions.add(ChatActionDto.builder()
            .label("🗓️ Full Calendar")
            .url("/calendar")
            .build());
    }

    private void appendPendingInquiries(StringBuilder sb, Object data, List<ChatActionDto> actions) {
        sb.append("### ✉️ Public Inquiries & Admissions Requests\n");
        if (data instanceof Map) {
            Map m = (Map) data;
            Object tot = m.get("totalInquiries") != null ? m.get("totalInquiries") : 0;
            Object pend = m.get("pendingInquiries") != null ? m.get("pendingInquiries") : 0;
            sb.append("• **Total Inquiries Received:** **").append(tot).append("**\n");
            sb.append("• **Pending Response:** **").append(pend).append("**\n\n");
        } else if (data instanceof List) {
            List list = (List) data;
            sb.append("• **Pending Inquiries Awaiting Response:** **").append(list.size()).append("**\n\n");
        }
        actions.add(ChatActionDto.builder()
            .label("✉️ View Inquiries")
            .url("/admin/inquiries")
            .build());
    }

    private AIResponse formatHelpfulUnknownResponse(String role, String originalQuery) {
        StringBuilder sb = new StringBuilder();
        List<ChatActionDto> actions = new ArrayList<>();

        if ("ADMIN".equalsIgnoreCase(role)) {
            sb.append("I couldn't find specific school ERP records matching **\"").append(originalQuery).append("\"**.\n\n");
            sb.append("As an Administrator, you can ask me directly about:\n");
            sb.append("• **Student Records:** *'student statistics'*, *'Class 10 students'*, or search any student (e.g. *'student Rahul'* or admission number).\n");
            sb.append("• **Attendance Metrics:** *'today's attendance'*, *'Class 10-A attendance'*, or overall school attendance.\n");
            sb.append("• **Fee Telemetry:** *'fee collections'*, *'fee analytics'*, or *'fee structure' class-wise.\n");
            sb.append("• **Faculty Overview:** *'faculty summary'*, or search for a teacher by name or subject.\n");
            sb.append("• **Notices & Events:** *'latest notices'*, *'upcoming school events'*, or school calendar.\n\n");
            sb.append("Quick links to ERP management modules:");

            actions.add(ChatActionDto.builder().label("👥 Students").url("/admin/students").build());
            actions.add(ChatActionDto.builder().label("📊 Attendance").url("/admin/attendance").build());
            actions.add(ChatActionDto.builder().label("💳 Fees").url("/admin/fees").build());
            actions.add(ChatActionDto.builder().label("👨‍🏫 Faculty").url("/admin/faculty").build());
        } else if ("TEACHER".equalsIgnoreCase(role)) {
            sb.append("I couldn't find specific school records matching **\"").append(originalQuery).append("\"**.\n\n");
            sb.append("As a Teacher, you can ask me about:\n");
            sb.append("• **My Classes & Students:** *'my classes'*, *'my students'*, or *'Class 10 students'*.\n");
            sb.append("• **Class Attendance:** *'Class 10-A attendance'*, or daily attendance register.\n");
            sb.append("• **Timetable:** *'my timetable today'*, or period timings.\n");
            sb.append("• **Circulars:** *'latest notices'*, *'upcoming events'*.\n");

            actions.add(ChatActionDto.builder().label("📚 My Classes").url("/teacher/classes").build());
            actions.add(ChatActionDto.builder().label("✍️ Mark Attendance").url("/teacher/attendance").build());
            actions.add(ChatActionDto.builder().label("⏰ Timetable").url("/teacher/timetable").build());
        } else {
            sb.append("I couldn't find specific school records matching **\"").append(originalQuery).append("\"**.\n\n");
            sb.append("You can ask me directly about:\n");
            sb.append("• **My Attendance:** *'what is my attendance percentage'* or *'days absent'*.\n");
            sb.append("• **My Fees:** *'pending fees balance'* or *'fee summary'*.\n");
            sb.append("• **My Schedule:** *'my timetable today'* or *'next period'*.\n");
            sb.append("• **School Updates:** *'latest notices'* or *'upcoming holidays & events'*.\n");

            actions.add(ChatActionDto.builder().label("📊 Check Attendance").url("/student/attendance").build());
            actions.add(ChatActionDto.builder().label("💳 View Fees").url("/student/fees").primary(true).build());
            actions.add(ChatActionDto.builder().label("📅 Timetable").url("/student/academics").build());
        }

        return AIResponse.builder()
            .content(sb.toString())
            .toolUsed("SchoolAssistantHelp")
            .toolsInvoked(Collections.singletonList("SchoolAssistantHelp"))
            .actions(actions)
            .fallbackUsed(true)
            .build();
    }

    private String formatINR(long amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(INDIA_LOCALE);
        return nf.format(amount).replace("INR", "₹").trim();
    }

    private long toLong(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
