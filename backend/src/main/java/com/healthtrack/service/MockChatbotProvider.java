package com.healthtrack.service;

import com.healthtrack.dto.ChatbotDtos.ChatRequest;
import com.healthtrack.dto.ChatbotDtos.ChatResponse;
import com.healthtrack.dto.ChatbotDtos.ExtractedData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockChatbotProvider implements ChatbotProvider {

    // TODO: (Part D.2) Replace this mock with a real LLM integration (OpenAI, Anthropic, or Spring AI).
    // Needs explicit decision on:
    // 1. Which vendor?
    // 2. How is the API key stored? (Must be AWS Secrets Manager or secure env var)
    // 3. Legal/Security review: patients typing symptoms is unauthenticated PHI.

    // Constraint: System-level instructions should scope it to hospital FAQs and basic pre-registration intake only.
    // Explicitly never producing a diagnosis.

    @Override
    public ChatResponse processChat(ChatRequest request) {
        String message = request.message().toLowerCase();
        
        // Very rudimentary mock logic for testing the queue integration
        String reply;
        String name = null;
        Integer age = null;
        List<String> symptoms = List.of();

        if (message.contains("headache")) {
            reply = "I understand you have a headache. I have logged this for the front desk. A staff member will call you shortly.";
            symptoms = List.of("headache");
        } else if (message.contains("fever")) {
            reply = "Fever noted. Please ensure you stay hydrated. I have forwarded your details to the triage queue.";
            symptoms = List.of("fever");
        } else {
            reply = "Thank you for reaching out. Please describe your symptoms or ask a question about hospital services, and I'll direct you to the right place. I cannot provide a medical diagnosis.";
        }

        ExtractedData data = new ExtractedData(name, age, symptoms);
        return new ChatResponse(reply, data);
    }
}
