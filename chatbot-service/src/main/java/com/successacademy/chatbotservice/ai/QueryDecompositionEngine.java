package com.successacademy.chatbotservice.ai;

import com.successacademy.chatbotservice.service.ContextManager;
import com.successacademy.chatbotservice.tools.ToolDefinition;
import com.successacademy.chatbotservice.tools.ToolRegistry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryDecompositionEngine {

    private final ToolRegistry toolRegistry;
    private final FuzzySpellCorrectionService spellCorrectionService;
    private final ContextManager contextManager;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubIntent {
        private String rawSnippet;
        private String domain; // STUDENTS, ATTENDANCE, FEES, TIMETABLE, TEACHERS, NOTICES, EVENTS, FACILITIES, CALENDAR, INQUIRIES
        private String toolName;
        private Map<String, Object> parameters;
        private boolean authorized;
        private String denialReason;
        private boolean isWriteOperation;
        private Double numericThreshold;
        private String thresholdOperator; // "<", ">", "<=", ">="
        private String thresholdField;    // "attendance", "fee", "days"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DecomposedQueryPlan {
        private String originalMessage;
        private String normalizedMessage;
        private String extractedClass;
        private String extractedSection;
        private String extractedStudentQuery;
        private String extractedTeacherQuery;
        private String extractedDateWindow;
        private String extractedFacility;
        private boolean isCompound;
        private boolean isFollowUp;
        private boolean isWriteOperation;
        private List<SubIntent> intents;
        private List<String> unresolvedQuestions;
    }

    private static final Pattern CLASS_SEC_PATTERN = Pattern.compile(
        "(?i)\\b(?:class\\s*)?([1-9]|1[0-2])\\s*[-/ ]?\\s*([a-dA-D])\\b"
    );

    private static final Pattern CLASS_ONLY_PATTERN = Pattern.compile(
        "(?i)\\b(?:class|grade|standard)\\s*([1-9]|1[0-2])\\b"
    );

    private static final Pattern PERCENT_THRESHOLD_PATTERN = Pattern.compile(
        "(?i)(?:below|less than|kam|<)\\s*(\\d{1,3})\\s*%"
    );

    private static final Pattern FEE_THRESHOLD_PATTERN = Pattern.compile(
        "(?i)(?:above|more than|greater than|>)\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+)"
    );

    private static final Pattern WRITE_OPERATION_PATTERN = Pattern.compile(
        "(?i)\\b(?:mark\\s+(?:[\\w\\s]{1,20}\\s+)?(?:absent|present)|mark\\s+attendance|take\\s+attendance|record\\s+attendance|update\\s+(?:fee|attendance|marks|grade|profile)|change\\s+(?:fee|attendance|marks|grade)|delete\\s+(?:fee|student|teacher|record|attendance|invoice)|cancel\\s+(?:fee|invoice|receipt)|remove\\s+(?:student|teacher|record)|add\\s+(?:student|teacher|fee|record)|insert\\s+(?:student|record)|edit\\s+(?:student|fee|attendance)|set\\s+attendance|give\\s+concession)\\b"
    );

    public DecomposedQueryPlan decompose(String userMessage,
                                         Long conversationId,
                                         Long userId,
                                         String role,
                                         Long studentId,
                                         Long teacherId) {

        String r = (role != null ? role.toUpperCase() : "STUDENT");
        String raw = userMessage.trim();
        String norm = spellCorrectionService.normalizeAndCorrect(raw.toLowerCase());

        ContextManager.ConversationContext ctx = contextManager.getContext(conversationId);

        // 1. Entity Extraction
        String detectedClass = null;
        String detectedSection = null;

        Matcher csMatcher = CLASS_SEC_PATTERN.matcher(raw);
        if (csMatcher.find()) {
            detectedClass = csMatcher.group(1);
            detectedSection = csMatcher.group(2).toUpperCase();
        } else {
            Matcher cMatcher = CLASS_ONLY_PATTERN.matcher(raw);
            if (cMatcher.find()) {
                detectedClass = cMatcher.group(1);
            }
        }

        // Contextual follow-up resolution
        boolean isFollowUp = false;
        if (detectedClass == null && ctx.getLastClass() != null) {
            // Check if user is asking something that implies a class
            if (norm.contains("student") || norm.contains("attend") || norm.contains("absent") || norm.contains("timetable") || norm.contains("how many")) {
                detectedClass = ctx.getLastClass();
                detectedSection = ctx.getLastSection();
                isFollowUp = true;
                log.info("Resolved class from conversational context: {}-{}", detectedClass, detectedSection);
            }
        }

        // Time window extraction
        String dateWindow = extractDateWindow(norm, raw);
        if (dateWindow == null && ctx.getLastDateWindow() != null && isFollowUpQuestion(norm)) {
            dateWindow = ctx.getLastDateWindow();
        }

        // Facility detection
        String facility = extractFacilityKeyword(norm);

        // Check write operation
        boolean isWrite = isWriteAttempt(norm) || isWriteAttempt(raw);

        // Update context with newly discovered entities
        if (detectedClass != null || dateWindow != null) {
            contextManager.updateContext(conversationId, ContextManager.ConversationContext.builder()
                .lastClass(detectedClass)
                .lastSection(detectedSection)
                .lastDateWindow(dateWindow)
                .build());
        }

        // 2. Identify Sub-Intents
        List<SubIntent> intents = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();

        if (isWrite) {
            intents.add(SubIntent.builder()
                .rawSnippet(raw)
                .isWriteOperation(true)
                .authorized(false)
                .denialReason("I am an information assistant and read-only for ERP data. System modifications must be performed directly through the authorized ERP screens.")
                .build());
            return DecomposedQueryPlan.builder()
                .originalMessage(raw)
                .normalizedMessage(norm)
                .isWriteOperation(true)
                .intents(intents)
                .build();
        }

        // Check for Attendance intent
        if (hasAttendanceIntent(norm, raw)) {
            SubIntent attIntent = buildAttendanceSubIntent(r, studentId, detectedClass, detectedSection, dateWindow, norm);
            intents.add(attIntent);
        }

        // Check for Fee intent
        if (hasFeeIntent(norm, raw)) {
            SubIntent feeIntent = buildFeeSubIntent(r, studentId, detectedClass, detectedSection, norm);
            intents.add(feeIntent);
        }

        // Check for Timetable intent
        if (hasTimetableIntent(norm, raw)) {
            SubIntent timeIntent = buildTimetableSubIntent(r, studentId, teacherId, detectedClass, detectedSection, dateWindow, norm);
            intents.add(timeIntent);
        }

        // Check for Student Roster / Count intent
        if (hasStudentRosterIntent(norm, raw, detectedClass)) {
            SubIntent stuIntent = buildStudentRosterSubIntent(r, detectedClass, detectedSection, norm);
            intents.add(stuIntent);
        }

        // Check for Teacher / Faculty intent
        if (hasTeacherIntent(norm, raw)) {
            SubIntent teacherIntent = buildTeacherSubIntent(r, userId, teacherId, raw, norm);
            intents.add(teacherIntent);
        }

        // Check for Notices / Circulars intent
        if (hasNoticeIntent(norm, raw)) {
            intents.add(SubIntent.builder()
                .rawSnippet("notices")
                .domain("NOTICES")
                .toolName("getLatestNotices")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build());
        }

        // Check for Events intent
        if (hasEventIntent(norm, raw)) {
            intents.add(SubIntent.builder()
                .rawSnippet("events")
                .domain("EVENTS")
                .toolName("getUpcomingEvents")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build());
        }

        // Check for School Facilities intent
        if (facility != null || norm.contains("facility") || norm.contains("infrastructure") || norm.contains("campus") || norm.contains("bus") || norm.contains("transport")) {
            intents.add(SubIntent.builder()
                .rawSnippet(facility != null ? facility : "facilities")
                .domain("FACILITIES")
                .toolName("getSchoolFacilities")
                .parameters(facility != null ? Map.of("facilityType", facility) : Collections.emptyMap())
                .authorized(true)
                .build());
        }

        // Check for Academic Calendar / Holiday intent
        if (hasCalendarIntent(norm, raw)) {
            intents.add(SubIntent.builder()
                .rawSnippet("academic calendar")
                .domain("CALENDAR")
                .toolName("getAcademicCalendar")
                .parameters(dateWindow != null ? Map.of("query", dateWindow) : Collections.emptyMap())
                .authorized(true)
                .build());
        }

        // Check for Admin General Operational Summary intent
        if ("ADMIN".equals(r) && (norm.contains("operational summary") || norm.contains("overview of school") || norm.contains("school summary") || norm.contains("overall status") || norm.contains("kya important hai"))) {
            if (intents.isEmpty()) {
                intents.add(SubIntent.builder().domain("STUDENTS").toolName("getStudentStatistics").parameters(Collections.emptyMap()).authorized(true).build());
                intents.add(SubIntent.builder().domain("TEACHERS").toolName("getFacultySummary").parameters(Collections.emptyMap()).authorized(true).build());
                intents.add(SubIntent.builder().domain("ATTENDANCE").toolName("getAttendanceAnalytics").parameters(Collections.emptyMap()).authorized(true).build());
                intents.add(SubIntent.builder().domain("FEES").toolName("getFeeAnalytics").parameters(Collections.emptyMap()).authorized(true).build());
                intents.add(SubIntent.builder().domain("INQUIRIES").toolName("getPendingInquiries").parameters(Collections.emptyMap()).authorized(true).build());
            }
        }

        // Cross-Student IDOR Guard for Students
        if ("STUDENT".equals(r)) {
            for (SubIntent intent : intents) {
                if (isCrossStudentAttempt(raw, norm)) {
                    intent.setAuthorized(false);
                    intent.setDenialReason("I can only provide information for your own authenticated student account. Access to other students' private records is restricted.");
                }
            }
        }

        return DecomposedQueryPlan.builder()
            .originalMessage(raw)
            .normalizedMessage(norm)
            .extractedClass(detectedClass)
            .extractedSection(detectedSection)
            .extractedDateWindow(dateWindow)
            .extractedFacility(facility)
            .isCompound(intents.size() > 1)
            .isFollowUp(isFollowUp)
            .isWriteOperation(false)
            .intents(intents)
            .unresolvedQuestions(unresolved)
            .build();
    }

    private SubIntent buildAttendanceSubIntent(String role, Long studentId, String className, String section, String dateWindow, String norm) {
        if ("STUDENT".equals(role)) {
            return SubIntent.builder()
                .domain("ATTENDANCE")
                .toolName("getMyAttendance")
                .parameters(dateWindow != null ? Map.of("dateWindow", dateWindow) : Collections.emptyMap())
                .authorized(true)
                .build();
        } else if ("TEACHER".equals(role)) {
            String c = className != null ? className : "10";
            String s = section != null ? section : "A";
            return SubIntent.builder()
                .domain("ATTENDANCE")
                .toolName("getClassAttendance")
                .parameters(Map.of("studentClass", c, "section", s))
                .authorized(true)
                .build();
        } else {
            // Admin
            if (className != null) {
                return SubIntent.builder()
                    .domain("ATTENDANCE")
                    .toolName("getClassAttendance")
                    .parameters(Map.of("studentClass", className, "section", section != null ? section : "A"))
                    .authorized(true)
                    .build();
            }
            return SubIntent.builder()
                .domain("ATTENDANCE")
                .toolName("getAttendanceAnalytics")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build();
        }
    }

    private SubIntent buildFeeSubIntent(String role, Long studentId, String className, String section, String norm) {
        if ("STUDENT".equals(role)) {
            return SubIntent.builder()
                .domain("FEES")
                .toolName("getMyFees")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build();
        } else if ("ADMIN".equals(role)) {
            if (norm.contains("structure") || norm.contains("schedule") || norm.contains("standard")) {
                return SubIntent.builder()
                    .domain("FEES")
                    .toolName("getFeeStructures")
                    .parameters(className != null ? Map.of("className", className) : Collections.emptyMap())
                    .authorized(true)
                    .build();
            }
            return SubIntent.builder()
                .domain("FEES")
                .toolName("getFeeAnalytics")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build();
        } else {
            // Teacher requesting fee info
            return SubIntent.builder()
                .domain("FEES")
                .toolName("getFeeStructures")
                .parameters(className != null ? Map.of("className", className) : Collections.emptyMap())
                .authorized(true)
                .build();
        }
    }

    private SubIntent buildTimetableSubIntent(String role, Long studentId, Long teacherId, String className, String section, String dateWindow, String norm) {
        if ("STUDENT".equals(role)) {
            return SubIntent.builder()
                .domain("TIMETABLE")
                .toolName("getMyTimetable")
                .parameters(dateWindow != null ? Map.of("dayOfWeek", dateWindow) : Collections.emptyMap())
                .authorized(true)
                .build();
        } else if ("TEACHER".equals(role)) {
            if (className != null) {
                return SubIntent.builder()
                    .domain("TIMETABLE")
                    .toolName("getClassTimetable")
                    .parameters(Map.of("studentClass", className, "section", section != null ? section : "A"))
                    .authorized(true)
                    .build();
            }
            return SubIntent.builder()
                .domain("TIMETABLE")
                .toolName("getTeacherSchedule")
                .parameters(dateWindow != null ? Map.of("dayOfWeek", dateWindow) : Collections.emptyMap())
                .authorized(true)
                .build();
        } else {
            // Admin
            String c = className != null ? className : "10";
            String s = section != null ? section : "A";
            return SubIntent.builder()
                .domain("TIMETABLE")
                .toolName("getClassTimetable")
                .parameters(Map.of("studentClass", c, "section", s))
                .authorized(true)
                .build();
        }
    }

    private SubIntent buildStudentRosterSubIntent(String role, String className, String section, String norm) {
        if ("STUDENT".equals(role)) {
            return SubIntent.builder()
                .domain("STUDENTS")
                .toolName("getMyProfile")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build();
        }
        if (className != null) {
            return SubIntent.builder()
                .domain("STUDENTS")
                .toolName("getClassStudents")
                .parameters(Map.of("className", className, "section", section != null ? section : "A"))
                .authorized(true)
                .build();
        }
        return SubIntent.builder()
            .domain("STUDENTS")
            .toolName("getStudentStatistics")
            .parameters(Collections.emptyMap())
            .authorized("ADMIN".equals(role))
            .denialReason(!"ADMIN".equals(role) ? "Please specify which class students you would like to view." : null)
            .build();
    }

    private SubIntent buildTeacherSubIntent(String role, Long userId, Long teacherId, String raw, String norm) {
        if ("STUDENT".equals(role)) {
            return SubIntent.builder()
                .domain("TEACHERS")
                .toolName("searchTeacher")
                .parameters(Map.of("query", raw))
                .authorized(true)
                .build();
        } else if ("TEACHER".equals(role)) {
            if (norm.contains("my profile") || norm.contains("who am i")) {
                return SubIntent.builder()
                    .domain("TEACHERS")
                    .toolName("getMyTeacherProfile")
                    .parameters(Collections.emptyMap())
                    .authorized(true)
                    .build();
            }
            if (norm.contains("class") || norm.contains("assigned")) {
                return SubIntent.builder()
                    .domain("TEACHERS")
                    .toolName("getMyClasses")
                    .parameters(Collections.emptyMap())
                    .authorized(true)
                    .build();
            }
            return SubIntent.builder()
                .domain("TEACHERS")
                .toolName("getTeacherSchedule")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build();
        } else {
            // Admin
            return SubIntent.builder()
                .domain("TEACHERS")
                .toolName("getFacultySummary")
                .parameters(Collections.emptyMap())
                .authorized(true)
                .build();
        }
    }

    // ── Intent detection helpers ──

    private boolean hasAttendanceIntent(String norm, String raw) {
        return norm.contains("attend") || norm.contains("absent") || norm.contains("present") ||
               norm.contains("hazri") || norm.contains("roll call") || raw.contains("attend");
    }

    private boolean hasFeeIntent(String norm, String raw) {
        return norm.contains("fee") || norm.contains("pay") || norm.contains("due") ||
               norm.contains("balance") || norm.contains("receipt") || norm.contains("paisa") ||
               norm.contains("rupee") || raw.contains("fee") || raw.contains("fess");
    }

    private boolean hasTimetableIntent(String norm, String raw) {
        return norm.contains("timetable") || norm.contains("schedule") || norm.contains("period") ||
               norm.contains("class today") || norm.contains("classes today") || norm.contains("next period") ||
               norm.contains("first class") || norm.contains("next class");
    }

    private boolean hasStudentRosterIntent(String norm, String raw, String className) {
        return (className != null && (norm.contains("student") || norm.contains("kitne") || norm.contains("how many") || norm.contains("roster") || norm.contains("list"))) ||
               norm.contains("total student") || norm.contains("enrolled");
    }

    private boolean hasTeacherIntent(String norm, String raw) {
        return norm.contains("teacher") || norm.contains("faculty") || norm.contains("sir") ||
               norm.contains("madam") || norm.contains("prof") || norm.contains("educator");
    }

    private boolean hasNoticeIntent(String norm, String raw) {
        return norm.contains("notice") || norm.contains("circular") || norm.contains("announcement") ||
               norm.contains("update") || norm.contains("khabar");
    }

    private boolean hasEventIntent(String norm, String raw) {
        return norm.contains("event") || norm.contains("sports day") || norm.contains("annual day") ||
               norm.contains("celebration") || norm.contains("competition") || norm.contains("exhibition");
    }

    private boolean hasCalendarIntent(String norm, String raw) {
        return norm.contains("holiday") || norm.contains("vacation") || norm.contains("chutti") ||
               norm.contains("exam") || norm.contains("mid-term") || norm.contains("calendar") ||
               norm.contains("ptm");
    }

    private String extractDateWindow(String norm, String raw) {
        if (norm.contains("today") || norm.contains("aaj")) return "today";
        if (norm.contains("tomorrow") || norm.contains("kal")) return "tomorrow";
        if (norm.contains("september")) return "September";
        if (norm.contains("october")) return "October";
        if (norm.contains("november")) return "November";
        if (norm.contains("december")) return "December";
        if (norm.contains("january")) return "January";
        if (norm.contains("next week")) return "next week";
        return null;
    }

    private String extractFacilityKeyword(String norm) {
        if (norm.contains("computer") || norm.contains("comp lab")) return "computer lab";
        if (norm.contains("science lab") || norm.contains("physics lab") || norm.contains("chemistry lab") || norm.contains("biology lab")) return "science lab";
        if (norm.contains("library") || norm.contains("book")) return "library";
        if (norm.contains("sport") || norm.contains("ground") || norm.contains("cricket") || norm.contains("football")) return "sports";
        if (norm.contains("bus") || norm.contains("transport") || norm.contains("route")) return "bus";
        if (norm.contains("smart class") || norm.contains("classroom")) return "smart classrooms";
        return null;
    }

    private boolean isWriteAttempt(String query) {
        if (query == null) return false;
        if (WRITE_OPERATION_PATTERN.matcher(query).find()) return true;
        String q = query.toLowerCase();
        return q.contains("mark absent") || q.contains("mark present") || q.contains("delete") ||
               q.contains("update fee") || q.contains("change fee") || q.contains("add student") ||
               q.contains("remove student") || q.contains("set attendance") || q.contains("give concession") ||
               (q.contains("mark") && (q.contains("absent") || q.contains("present")));
    }

    private boolean isCrossStudentAttempt(String raw, String norm) {
        // Checks if student is asking for someone else by name
        String[] otherNames = {"rahul", "priya", "arjun", "sneha", "amit", "vikram", "rohit", "pooja", "sunita"};
        for (String name : otherNames) {
            if (norm.contains(name) && (norm.contains("fee") || norm.contains("attend") || norm.contains("record"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFollowUpQuestion(String norm) {
        return norm.startsWith("and ") || norm.startsWith("how many ") || norm.startsWith("aur ") ||
               norm.startsWith("unka ") || norm.startsWith("what about ") || norm.contains("unme se");
    }
}
