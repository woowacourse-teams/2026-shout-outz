package com.shoutoutz.api.common.llm;

import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class GeminiLlmClient extends AbstractLlmClient {
    private final LlmProperties.Gemini properties;

    public GeminiLlmClient(RestClient restClient, LlmProperties.Gemini properties, ObjectMapper mapper) {
        super(restClient, mapper, properties.model());
        this.properties = properties;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.GEMINI;
    }

    @Override
    public String generate(LlmRequest request) {
        JsonNode response = post("/v1beta/models/" + model + ":generateContent", "x-goog-api-key",
                properties.apiKey(), Map.of(
                        "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", request.prompt())))),
                        "generationConfig", Map.of("responseMimeType", "application/json",
                                "responseJsonSchema", request.responseSchema(), "maxOutputTokens", request.maxOutputTokens())));
        if (response.path("promptFeedback").hasNonNull("blockReason")) {
            throw failure(200, "CONTENT_BLOCKED", false);
        }
        JsonNode candidate = response.path("candidates").path(0);
        String finish = candidate.path("finishReason").asString("");
        if (!"STOP".equals(finish)) {
            boolean blocked = List.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII").contains(finish);
            throw failure(200, blocked ? "CONTENT_BLOCKED" : "INCOMPLETE_RESPONSE", !blocked);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false)) {
                text.append(part.path("text").asString(""));
            }
        }
        if (text.toString().isBlank()) {
            throw failure(200, "EMPTY_RESPONSE", true);
        }
        return text.toString();
    }
}
