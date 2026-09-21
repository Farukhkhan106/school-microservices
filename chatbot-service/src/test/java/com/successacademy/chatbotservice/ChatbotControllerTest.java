package com.successacademy.chatbotservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.successacademy.chatbotservice.ai.AIProvider;
import com.successacademy.chatbotservice.ai.AIResponse;
import com.successacademy.chatbotservice.dto.ChatRequest;
import com.successacademy.chatbotservice.model.ChatConversation;
import com.successacademy.chatbotservice.repository.ChatConversationRepository;
import com.successacademy.chatbotservice.repository.ChatMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @MockBean
    private AIProvider aiProvider;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();

        Mockito.when(aiProvider.chat(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(AIResponse.builder()
                        .content("Your attendance is 95%.")
                        .toolUsed("getMyAttendance")
                        .fallbackUsed(true)
                        .build());
    }

    @Test
    @DisplayName("P0: Missing JWT / Unauthenticated request must return 401 Unauthorized")
    void testUnauthenticatedChat() throws Exception {
        ChatRequest request = new ChatRequest("What is my attendance?", null);

        mockMvc.perform(post("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("P0: Authenticated Student Chat succeeds and stores message")
    void testAuthenticatedStudentChat() throws Exception {
        ChatRequest request = new ChatRequest("What is my attendance?", null);

        mockMvc.perform(post("/chat")
                .header("X-User-Id", "101")
                .header("X-User-Role", "STUDENT")
                .header("X-Student-Id", "55")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your attendance is 95%."))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.toolUsed").value("getMyAttendance"));
    }

    @Test
    @DisplayName("P0: IDOR Security - Student A cannot access Student B's conversation")
    void testIDORConversationAccess() throws Exception {
        // Create conversation belonging to User 200
        ChatConversation conv = conversationRepository.save(ChatConversation.builder()
                .userId(200L)
                .role("STUDENT")
                .title("Private Chat")
                .build());

        // User 101 attempts to read User 200's conversation
        mockMvc.perform(get("/chat/conversations/" + conv.getId())
                .header("X-User-Id", "101")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("P0: Owner can access their own conversation")
    void testOwnerAccessesOwnConversation() throws Exception {
        ChatConversation conv = conversationRepository.save(ChatConversation.builder()
                .userId(101L)
                .role("STUDENT")
                .title("My Chat")
                .build());

        mockMvc.perform(get("/chat/conversations/" + conv.getId())
                .header("X-User-Id", "101")
                .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conv.getId()))
                .andExpect(jsonPath("$.title").value("My Chat"));
    }
}
