package com.shoutoutz.api.common.llm;

import java.util.Map;

public record LlmRequest(String prompt, Map<String, Object> responseSchema, int maxOutputTokens) {
    public LlmRequest {
        if (prompt == null || prompt.isBlank() || responseSchema == null || maxOutputTokens < 1) {
            throw new IllegalArgumentException("LLM 요청의 프롬프트, 응답 스키마와 출력 한도를 설정해야 합니다.");
        }
        responseSchema = Map.copyOf(responseSchema);
    }

    @Override
    public String toString() {
        return "LlmRequest[maxOutputTokens=" + maxOutputTokens + "]";
    }
}
