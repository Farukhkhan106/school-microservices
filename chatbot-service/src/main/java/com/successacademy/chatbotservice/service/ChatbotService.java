package com.successacademy.chatbotservice.service;

import com.successacademy.chatbotservice.dto.ChatRequest;
import com.successacademy.chatbotservice.dto.ChatResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ChatbotService {

    ChatResponse processChat(ChatRequest request, HttpServletRequest httpRequest);
}
