package com.healthtrack.service;

import com.healthtrack.dto.ChatbotDtos.ChatRequest;
import com.healthtrack.dto.ChatbotDtos.ChatResponse;

public interface ChatbotProvider {
    ChatResponse processChat(ChatRequest request);
}
