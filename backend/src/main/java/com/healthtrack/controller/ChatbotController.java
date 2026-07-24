package com.healthtrack.controller;

import com.healthtrack.dto.ChatbotDtos.ChatRequest;
import com.healthtrack.dto.ChatbotDtos.ChatResponse;
import com.healthtrack.service.ChatbotProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotProvider chatbotProvider;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatbotProvider.processChat(request));
    }
}
