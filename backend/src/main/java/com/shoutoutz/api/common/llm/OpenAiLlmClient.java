package com.shoutoutz.api.common.llm;

import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class OpenAiLlmClient extends AbstractLlmClient {
    private final LlmProperties.OpenAi properties;

    public OpenAiLlmClient(RestClient restClient, LlmProperties.OpenAi properties, ObjectMapper mapper) {
        super(restClient, mapper, properties.model());
        this.properties = properties;
    }

    @Override
    public LlmProvider provider() {
        return LlmProvider.OPENAI;
    }

    @Override
    public String generate(LlmRequest request) {
        String key = properties.apiKey();
        if (key == null || key.isBlank()) {
            throw failure(null, "MISSING_API_KEY", false);
        }
        JsonNode response = post("/v1/responses", HttpHeaders.AUTHORIZATION, "Bearer " + key,
                Map.of("model", model, "input", request.prompt(), "store", false,
                        "reasoning", Map.of("effort", properties.reasoningEffort()),
                        "max_output_tokens", request.maxOutputTokens(),
                        "text", Map.of("format", Map.of("type", "json_schema", "name", "structured_response",
                                "strict", true, "schema", request.responseSchema()))));
        String status = response.path("status").asString("");
        if (!"completed".equals(status)) {
            boolean blocked = "content_filter".equals(response.path("incomplete_details").path("reason").asString());
            throw failure(200, blocked ? "CONTENT_BLOCKED" : "INCOMPLETE_RESPONSE", !blocked);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asString())) {
                continue;
            }
            for (JsonNode part : output.path("content")) {
                if ("refusal".equals(part.path("type").asString())) {
                    throw failure(200, "REFUSAL", false);
                }
                if ("output_text".equals(part.path("type").asString())) {
                    text.append(part.path("text").asString(""));
                }
            }
        }
        if (text.toString().isBlank()) {
            throw failure(200, "EMPTY_RESPONSE", true);
        }
        return text.toString();
    }
}
