package com.healthtrack.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ChatbotDtos {

    public record ChatRequest(
            Long hospitalId,
            @NotBlank String message,
            String sessionId
    ) {}

    public record ExtractedData(
            String name,
            Integer age,
            List<String> symptoms
    ) {}

    public record ChatResponse(
            String reply,
            ExtractedData extractedData
    ) {}
}
