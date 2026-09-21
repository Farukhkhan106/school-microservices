package com.successacademy.chatbotservice.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    public ToolRegistry() {
        registerDefaultTools();
    }

    public void registerTool(ToolDefinition tool) {
        tools.put(tool.getName(), tool);
        log.info("Registered tool: [{}] under domain [{}]", tool.getName(), tool.getDomain());
    }

    public ToolDefinition getTool(String name) {
        return tools.get(name);
    }

    public List<ToolDefinition> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    public List<ToolDefinition> getToolsForRole(String role) {
        String r = (role != null ? role.toUpperCase() : "STUDENT");
        List<ToolDefinition> allowed = new ArrayList<>();
        for (ToolDefinition t : tools.values()) {
            if (t.getAllowedRoles() != null && (t.getAllowedRoles().contains("ALL") || t.getAllowedRoles().contains(r))) {
                allowed.add(t);
            }
        }
        return allowed;
    }

    public boolean isRoleAuthorized(String toolName, String role) {
        ToolDefinition tool = tools.get(toolName);
        if (tool == null) return false;
        String r = (role != null ? role.toUpperCase() : "STUDENT");
        return tool.getAllowedRoles() != null && (tool.getAllowedRoles().contains("ALL") || tool.getAllowedRoles().contains(r));
    }

    private void registerDefaultTools() {
        // ── STUDENT DOMAIN ──
        registerTool(ToolDefinition.builder()
            .name("getMyProfile")
            .domain("STUDENTS")
            .description("Retrieves the authenticated student's profile (admission number, roll number, class, section, parent details, date of birth, contact).")
            .allowedRoles(List.of("STUDENT"))
            .parameters(Collections.emptyMap())
            .build());

        registerTool(ToolDefinition.builder()
            .name("searchStudent")
            .domain("STUDENTS")
            .description("Searches for a student by name or admission number and retrieves academic and profile details.")
            .allowedRoles(List.of("ADMIN", "TEACHER"))
            .parameters(Map.of("query", "Student name or admission number"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getClassStudents")
            .domain("STUDENTS")
            .description("Retrieves the full roster of students enrolled in a specific class and section (e.g., Class 10-A).")
            .allowedRoles(List.of("ADMIN", "TEACHER"))
            .parameters(Map.of("studentClass", "Class number/name", "section", "Section letter"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getStudentStatistics")
            .domain("STUDENTS")
            .description("Retrieves total student enrollment count, active vs inactive count, and grade-wise student breakdown.")
            .allowedRoles(List.of("ADMIN"))
            .parameters(Collections.emptyMap())
            .build());

        // ── ATTENDANCE DOMAIN ──
        registerTool(ToolDefinition.builder()
            .name("getMyAttendance")
            .domain("ATTENDANCE")
            .description("Retrieves the authenticated student's overall attendance rate (%), total present, absent, and late days. Attendance is daily school attendance.")
            .allowedRoles(List.of("STUDENT"))
            .parameters(Collections.emptyMap())
            .build());

        registerTool(ToolDefinition.builder()
            .name("getDetailedAttendance")
            .domain("ATTENDANCE")
            .description("Retrieves detailed date-by-date attendance records for a specific month or date range for an authorized student.")
            .allowedRoles(List.of("STUDENT", "TEACHER", "ADMIN"))
            .parameters(Map.of("studentId", "Student ID", "month", "Month name or number", "year", "Academic year"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getClassAttendance")
            .domain("ATTENDANCE")
            .description("Retrieves today's class attendance status, showing present, absent, and unmarked students for a specific class and section.")
            .allowedRoles(List.of("TEACHER", "ADMIN"))
            .parameters(Map.of("studentClass", "Class number", "section", "Section letter"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getAttendanceAnalytics")
            .domain("ATTENDANCE")
            .description("Retrieves school-wide daily attendance telemetry: total present, absent, overall attendance rate %, and classes with pending attendance.")
            .allowedRoles(List.of("ADMIN"))
            .parameters(Collections.emptyMap())
            .build());

        // ── FEES DOMAIN ──
        registerTool(ToolDefinition.builder()
            .name("getMyFees")
            .domain("FEES")
            .description("Retrieves the authenticated student's fee overview: total annual fee, total paid, remaining outstanding balance in INR (₹), and due dates.")
            .allowedRoles(List.of("STUDENT"))
            .parameters(Collections.emptyMap())
            .build());

        registerTool(ToolDefinition.builder()
            .name("getStudentFeeHistory")
            .domain("FEES")
            .description("Retrieves transaction history, receipts, payment modes (Online/Cash), and dates of fee payments.")
            .allowedRoles(List.of("STUDENT", "ADMIN"))
            .parameters(Map.of("studentId", "Student ID"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getFeeStructures")
            .domain("FEES")
            .description("Retrieves standard official school fee schedules by class (tuition, transport, lab, library, sports fee).")
            .allowedRoles(List.of("ALL"))
            .parameters(Map.of("className", "Optional class name to filter"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getFeeAnalytics")
            .domain("FEES")
            .description("Retrieves school-wide revenue telemetry: total revenue collected, total outstanding balance, cleared accounts count, and pending accounts count in INR (₹).")
            .allowedRoles(List.of("ADMIN"))
            .parameters(Collections.emptyMap())
            .build());

        // ── TIMETABLE & SCHEDULE DOMAIN ──
        registerTool(ToolDefinition.builder()
            .name("getMyTimetable")
            .domain("TIMETABLE")
            .description("Retrieves scheduled periods, subjects, timings, and teachers for the student's class and section for today or a specific day.")
            .allowedRoles(List.of("STUDENT"))
            .parameters(Map.of("dayOfWeek", "Optional day name e.g. Monday"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getTeacherSchedule")
            .domain("TIMETABLE")
            .description("Retrieves the authenticated teacher's teaching timetable, upcoming periods, room/class assignments, and day schedule.")
            .allowedRoles(List.of("TEACHER"))
            .parameters(Map.of("dayOfWeek", "Optional day name e.g. Monday"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getClassTimetable")
            .domain("TIMETABLE")
            .description("Retrieves the timetable schedule for any specific class and section.")
            .allowedRoles(List.of("TEACHER", "ADMIN"))
            .parameters(Map.of("studentClass", "Class number", "section", "Section letter", "dayOfWeek", "Day name"))
            .build());

        // ── TEACHERS & FACULTY DOMAIN ──
        registerTool(ToolDefinition.builder()
            .name("getMyTeacherProfile")
            .domain("TEACHERS")
            .description("Retrieves the authenticated teacher's profile, qualifications, assigned subjects, and designated Class Teacher class if any.")
            .allowedRoles(List.of("TEACHER"))
            .parameters(Collections.emptyMap())
            .build());

        registerTool(ToolDefinition.builder()
            .name("getMyClasses")
            .domain("TEACHERS")
            .description("Retrieves the classes, sections, and subjects assigned to the authenticated teacher.")
            .allowedRoles(List.of("TEACHER"))
            .parameters(Collections.emptyMap())
            .build());

        registerTool(ToolDefinition.builder()
            .name("getMyStudents")
            .domain("TEACHERS")
            .description("Retrieves the list of students in the authenticated teacher's assigned classes.")
            .allowedRoles(List.of("TEACHER"))
            .parameters(Collections.emptyMap())
            .build());

        registerTool(ToolDefinition.builder()
            .name("searchTeacher")
            .domain("TEACHERS")
            .description("Searches for a teacher/educator by name or subject and retrieves their designation, qualifications, experience, and subjects.")
            .allowedRoles(List.of("ALL"))
            .parameters(Map.of("query", "Teacher name or subject"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getFacultySummary")
            .domain("TEACHERS")
            .description("Retrieves total faculty count, active educators, class teacher assignments, and department breakdown.")
            .allowedRoles(List.of("ADMIN"))
            .parameters(Collections.emptyMap())
            .build());

        // ── NOTICES, CIRCULARS & ANNOUNCEMENTS ──
        registerTool(ToolDefinition.builder()
            .name("getLatestNotices")
            .domain("NOTICES")
            .description("Retrieves active official school circulars, notices, exam schedules, and holiday announcements.")
            .allowedRoles(List.of("ALL"))
            .parameters(Map.of("category", "Optional category filter: Academic, Exam, Holiday, General"))
            .build());

        // ── EVENTS & CELEBRATIONS ──
        registerTool(ToolDefinition.builder()
            .name("getUpcomingEvents")
            .domain("EVENTS")
            .description("Retrieves upcoming sports tournaments, annual days, science exhibitions, and cultural celebrations with dates and venues.")
            .allowedRoles(List.of("ALL"))
            .parameters(Collections.emptyMap())
            .build());

        // ── SCHOOL KNOWLEDGE, FACILITIES & CALENDAR ──
        registerTool(ToolDefinition.builder()
            .name("getSchoolFacilities")
            .domain("FACILITIES")
            .description("Retrieves verified school infrastructure info: modern science laboratories, computer labs, library capacity, sports ground, transport bus routes, and smart classrooms.")
            .allowedRoles(List.of("ALL"))
            .parameters(Map.of("facilityType", "Optional facility keyword: computer lab, library, sports, science lab, bus"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getAcademicCalendar")
            .domain("CALENDAR")
            .description("Retrieves academic year terms, examination windows, vacations, gazetted holidays, and parent-teacher meeting schedules.")
            .allowedRoles(List.of("ALL"))
            .parameters(Map.of("query", "Optional month or event name"))
            .build());

        registerTool(ToolDefinition.builder()
            .name("getPendingInquiries")
            .domain("INQUIRIES")
            .description("Retrieves admissions and general public inquiries pending administrative action.")
            .allowedRoles(List.of("ADMIN"))
            .parameters(Collections.emptyMap())
            .build());
    }
}
