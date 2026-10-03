package com.successacademy.chatbotservice.service;

import com.successacademy.chatbotservice.ai.AIProvider;
import com.successacademy.chatbotservice.ai.AIResponse;
import com.successacademy.chatbotservice.dto.ChatMessageDto;
import com.successacademy.chatbotservice.dto.ChatRequest;
import com.successacademy.chatbotservice.dto.ChatResponse;
import com.successacademy.chatbotservice.model.ChatConversation;
import com.successacademy.chatbotservice.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final ConversationService conversationService;
    private final AIProvider aiProvider;

    @Override
    public ChatResponse processChat(ChatRequest request, HttpServletRequest httpRequest) {
        UserContext.requireAuthenticated(httpRequest);

        Long userId = UserContext.getUserId(httpRequest);
        String role = UserContext.getRole(httpRequest);
        Long studentId = UserContext.getStudentId(httpRequest);
        Long teacherId = UserContext.getTeacherId(httpRequest);

        log.info("Processing chat: userId={}, role={}, studentId={}, teacherId={}", userId, role, studentId, teacherId);

        // 1. Get or create conversation (checks ownership if conversationId provided)
        ChatConversation conversation = conversationService.getOrCreateConversation(
            request.getConversationId(), userId, role, request.getMessage()
        );

        // 2. Save incoming user message
        conversationService.saveMessage(conversation.getId(), "USER", request.getMessage(), null);

        // 3. Get recent conversation history
        List<ChatMessageDto> history = conversationService.getRecentHistory(conversation.getId(), 6);

        // 4. Construct System Prompt based on role
        String systemPrompt = buildSystemPrompt(role, userId, studentId, teacherId);

        // 5. Query AI Provider (with multi-tool calling or cognitive decomposition)
        AIResponse aiResp = aiProvider.chat(
            request.getMessage(),
            systemPrompt,
            history,
            userId,
            role,
            studentId,
            teacherId,
            conversation.getId()
        );

        // 6. Save assistant response
        conversationService.saveMessage(
            conversation.getId(),
            "ASSISTANT",
            aiResp.getContent(),
            aiResp.getToolUsed()
        );

        return ChatResponse.builder()
            .message(aiResp.getContent())
            .conversationId(conversation.getId())
            .role(role)
            .toolUsed(aiResp.getToolUsed())
            .toolsInvoked(aiResp.getToolsInvoked())
            .actions(aiResp.getActions())
            .timestamp(LocalDateTime.now())
            .build();
    }

    private String buildSystemPrompt(String role, Long userId, Long studentId, Long teacherId) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are the official Success Academy AI Assistant for Success Academy School ERP.\n");
        sb.append("You must provide helpful, factual, and concise answers to students, teachers, and school administrators.\n");
        sb.append("CRITICAL RULES:\n");
        sb.append("1. Answer ERP questions using authoritative data from available tools.\n");
        sb.append("2. Attendance is DAILY SCHOOL ATTENDANCE (not subject-wise). Never claim attendance is subject-wise.\n");
        sb.append("3. Format fee amounts in Indian Rupees (INR - ₹) and dates cleanly.\n");
        sb.append("4. Never fabricate or invent ERP data. If information is not available, state so clearly.\n");
        sb.append("5. You are strictly read-only for ERP data. Never perform modifications (e.g. marking attendance, editing fees).\n");
        sb.append("6. Strictly respect user privacy and role authorization.\n\n");

        if ("STUDENT".equalsIgnoreCase(role)) {
            sb.append("CURRENT USER ROLE: STUDENT (Student ID: ").append(studentId).append(")\n");
            sb.append("- You may only provide details for this student's own profile, attendance, fees, timetable, notices, and events.\n");
            sb.append("- Never disclose details of any other student or faculty member.\n");
        } else if ("TEACHER".equalsIgnoreCase(role)) {
            sb.append("CURRENT USER ROLE: TEACHER (User ID: ").append(userId).append(", Teacher ID: ").append(teacherId).append(")\n");
            sb.append("- You may answer questions regarding the teacher's assigned classes, students, timetable schedule, and class attendance.\n");
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            sb.append("CURRENT USER ROLE: ADMINISTRATOR\n");
            sb.append("- You have access to all-school statistics: total students, faculty overview, daily attendance telemetry, fee collections, notices, events, and inquiries.\n");
        }

        return sb.toString();
    }
}
