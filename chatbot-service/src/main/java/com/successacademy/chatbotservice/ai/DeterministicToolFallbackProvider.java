package com.successacademy.chatbotservice.ai;

import com.successacademy.chatbotservice.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeterministicToolFallbackProvider {

    private final QueryDecompositionEngine decompositionEngine;
    private final DataJoinerAndAggregator dataJoinerAndAggregator;
    private final AnswerComposer answerComposer;
    private final FuzzySpellCorrectionService spellCorrectionService;

    public AIResponse handleQuery(String userMessage,
                                  List<ChatMessageDto> history,
                                  Long userId,
                                  String role,
                                  Long studentId,
                                  Long teacherId) {
        return handleQuery(userMessage, history, userId, role, studentId, teacherId, null);
    }

    public AIResponse handleQuery(String userMessage,
                                  List<ChatMessageDto> history,
                                  Long userId,
                                  String role,
                                  Long studentId,
                                  Long teacherId,
                                  Long conversationId) {

        String rawQuery = userMessage != null ? userMessage.trim() : "";
        String normQuery = spellCorrectionService.normalizeAndCorrect(rawQuery.toLowerCase());
        String r = (role != null ? role.toUpperCase() : "STUDENT");

        log.info("Cognitive Engine processing query: role={}, userId={}, raw='{}'", r, userId, rawQuery);

        // 1. Check for conversational greetings first
        if (spellCorrectionService.isGreeting(rawQuery.toLowerCase()) || spellCorrectionService.isGreeting(normQuery)) {
            return formatGreeting(r);
        }

        // 2. Query Decomposition & Entity Extraction
        QueryDecompositionEngine.DecomposedQueryPlan plan = decompositionEngine.decompose(
            rawQuery, conversationId, userId, r, studentId, teacherId
        );

        // 3. Multi-Tool Execution & Data Aggregation
        DataJoinerAndAggregator.JoinedExecutionResult joinedResult = dataJoinerAndAggregator.executeAndAggregate(
            plan, userId, r, studentId, teacherId
        );

        // 4. Natural-Language Answer Composition + Actions
        return answerComposer.compose(joinedResult, r, rawQuery);
    }

    private AIResponse formatGreeting(String role) {
        String greeting;
        switch (role) {
            case "STUDENT":
                greeting = "Hello! I am your Success Academy AI Assistant. You can ask me any question about your attendance, fees, class timetable, upcoming events, or school announcements.";
                break;
            case "TEACHER":
                greeting = "Hello! I can help you with your assigned classes, students, teaching schedule, daily attendance status, and official school circulars.";
                break;
            case "ADMIN":
                greeting = "Hello Administrator! I am ready to assist you with school analytics, enrollment numbers, fee collection telemetry, faculty overviews, and daily operations.";
                break;
            default:
                greeting = "Hello! I am your Success Academy AI Assistant. How can I assist you with school information today?";
                break;
        }
        return AIResponse.builder()
            .content(greeting)
            .toolUsed("GreetingHandler")
            .fallbackUsed(true)
            .build();
    }
}
