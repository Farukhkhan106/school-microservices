package com.successacademy.chatbotservice;

import com.successacademy.chatbotservice.ai.*;
import com.successacademy.chatbotservice.service.ContextManager;
import com.successacademy.chatbotservice.tools.ToolExecutionService;
import com.successacademy.chatbotservice.tools.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

public class QueryDecompositionAndOrchestrationTest {

    private QueryDecompositionEngine decompositionEngine;
    private DataJoinerAndAggregator dataJoinerAndAggregator;
    private AnswerComposer answerComposer;
    private ContextManager contextManager;
    private FuzzySpellCorrectionService spellCorrectionService;
    private ToolExecutionService toolExecutionService;
    private DeterministicToolFallbackProvider provider;
    private ToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        toolRegistry = new ToolRegistry();
        contextManager = new ContextManager();
        spellCorrectionService = new FuzzySpellCorrectionService();
        decompositionEngine = new QueryDecompositionEngine(toolRegistry, spellCorrectionService, contextManager);

        toolExecutionService = Mockito.mock(ToolExecutionService.class);
        dataJoinerAndAggregator = new DataJoinerAndAggregator(toolExecutionService);
        answerComposer = new AnswerComposer();

        provider = new DeterministicToolFallbackProvider(
                decompositionEngine,
                dataJoinerAndAggregator,
                answerComposer,
                spellCorrectionService
        );
    }

    @Test
    @DisplayName("P0: Multi-intent query by Student decomposes into Attendance + Fees and synthesizes joined response with INR")
    void testStudentMultiIntentQuery() {
        Mockito.when(toolExecutionService.executeTool(eq("getMyAttendance"), any(), any(), any(), eq(55L), any()))
                .thenReturn("{\"attendancePercentage\": 92.5, \"presentDays\": 37, \"absentDays\": 3}");

        Mockito.when(toolExecutionService.executeTool(eq("getMyFees"), any(), any(), any(), eq(55L), any()))
                .thenReturn("[{\"feeType\": \"Tuition\", \"amount\": 15000, \"paidAmount\": 10500}]");

        String query = "Mera attendance aur fees summary do";
        AIResponse response = provider.handleQuery(
                query,
                Collections.emptyList(),
                101L,
                "STUDENT",
                55L,
                null,
                1L
        );

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertNotNull(response.getToolsInvoked());
        assertTrue(response.getToolsInvoked().contains("getMyAttendance"));
        assertTrue(response.getToolsInvoked().contains("getMyFees"));

        // Verify INR currency formatting and attendance rate
        assertTrue(response.getContent().contains("92.5%"));
        assertTrue(response.getContent().contains("₹4,500") || response.getContent().contains("₹4500"));

        // Verify interactive actions
        assertNotNull(response.getActions());
        assertFalse(response.getActions().isEmpty());
        assertTrue(response.getActions().stream().anyMatch(a -> a.getUrl().contains("/fees")));
    }

    @Test
    @DisplayName("P0: Admin 3-part compound query decomposes into Roster + Attendance + Timetable with class/section extraction")
    void testAdminCompoundQueryWithEntities() {
        Mockito.when(toolExecutionService.executeTool(eq("getClassStudents"), any(), any(), any(), any(), any()))
                .thenReturn("[{\"name\": \"Aarav Sharma\", \"rollNumber\": \"10A01\"}, {\"name\": \"Diya Patel\", \"rollNumber\": \"10A02\"}]");

        Mockito.when(toolExecutionService.executeTool(eq("getClassAttendance"), any(), any(), any(), any(), any()))
                .thenReturn("{\"presentCount\": 2, \"absentCount\": 0}");

        Mockito.when(toolExecutionService.executeTool(eq("getClassTimetable"), any(), any(), any(), any(), any()))
                .thenReturn("[{\"periodNo\": 1, \"subject\": \"Mathematics\", \"teacherName\": \"Mr. Sharma\", \"startTime\": \"09:00\", \"endTime\": \"09:45\"}]");

        String query = "Class 10-A me kitne students hain, aaj attendance kya hai aur timetable kya hai?";
        AIResponse response = provider.handleQuery(
                query,
                Collections.emptyList(),
                1L,
                "ADMIN",
                null,
                null,
                10L
        );

        assertNotNull(response);
        assertNotNull(response.getToolsInvoked());
        assertEquals(3, response.getToolsInvoked().size());
        assertTrue(response.getToolsInvoked().contains("getClassStudents"));
        assertTrue(response.getToolsInvoked().contains("getClassAttendance"));
        assertTrue(response.getToolsInvoked().contains("getClassTimetable"));

        // Verify synthesized response has content from all 3 tools
        assertTrue(response.getContent().contains("2 students"));
        assertTrue(response.getContent().contains("Present"));
        assertTrue(response.getContent().contains("Mathematics"));
    }

    @Test
    @DisplayName("P0: IDOR Protection - Student asking for another student's data is blocked")
    void testStudentIdorAttemptBlocked() {
        String query = "Show details for student 88 fees and marks";
        QueryDecompositionEngine.DecomposedQueryPlan plan = decompositionEngine.decompose(
                query,
                5L,
                101L,
                "STUDENT",
                55L, // Student ID is 55
                null
        );

        // SubIntents should either be unauthorized or sanitized to own studentId
        for (QueryDecompositionEngine.SubIntent intent : plan.getIntents()) {
            if ("FEES".equals(intent.getDomain())) {
                // Must be restricted to getMyFees or studentId 55, not 88
                assertEquals("getMyFees", intent.getToolName());
            }
        }
    }

    @Test
    @DisplayName("P0: Write Rejection Guard - AI assistant rejects modification requests (read-only)")
    void testWriteOperationRejected() {
        String query = "Mark Rahul absent for today in attendance register";
        AIResponse response = provider.handleQuery(
                query,
                Collections.emptyList(),
                101L,
                "TEACHER",
                null,
                12L,
                6L
        );

        assertNotNull(response);
        assertTrue(response.getContent().contains("read-only") || response.getContent().contains("modifications"));
    }

    @Test
    @DisplayName("P0: Multi-turn Conversation Context - Remembers Class 10-A across turns")
    void testMultiTurnContextPreservation() {
        // Turn 1: User asks about Class 10-A
        QueryDecompositionEngine.DecomposedQueryPlan plan1 = decompositionEngine.decompose(
                "How many students are in Class 10-A?",
                99L,
                1L,
                "ADMIN",
                null,
                null
        );
        assertEquals("10", plan1.getExtractedClass());
        assertEquals("A", plan1.getExtractedSection());

        // ContextManager now holds Class 10-A for conversation 99
        ContextManager.ConversationContext ctx = contextManager.getContext(99L);
        assertEquals("10", ctx.getLastClass());
        assertEquals("A", ctx.getLastSection());

        // Turn 2: Follow-up query without explicitly mentioning class or section: "What is their timetable today?"
        QueryDecompositionEngine.DecomposedQueryPlan plan2 = decompositionEngine.decompose(
                "What is their timetable today?",
                99L,
                1L,
                "ADMIN",
                null,
                null
        );
        // Follow-up inherits Class 10-A from session context
        assertEquals("10", plan2.getExtractedClass());
        assertEquals("A", plan2.getExtractedSection());
    }

    @Test
    @DisplayName("P0: Ungrounded / Unknown Information - Clearly reports missing ERP data without hallucinating")
    void testUngroundedQueryReportedFactually() {
        String query = "What is the principal's personal smartphone phone number?";
        AIResponse response = provider.handleQuery(
                query,
                Collections.emptyList(),
                101L,
                "STUDENT",
                55L,
                null,
                7L
        );

        assertNotNull(response);
        // Should clearly state lack of information and offer official contact
        assertTrue(response.getContent().contains("couldn't find") ||
                   response.getContent().contains("school ERP data") ||
                   response.getContent().contains("administrative office"));
    }
}
