package com.shoutoutz.api.common.llm;

import java.util.Map;

/**
 * temperature가 null이면 제공자 기본값을 사용한다. OpenAI 추론 모델은 sampling 파라미터를 받지 않아 무시한다.
 */
public record LlmRequest(String prompt, Map<String, Object> responseSchema, int maxOutputTokens, Double temperature) {
    public LlmRequest {
        if (prompt == null || prompt.isBlank() || responseSchema == null || maxOutputTokens < 1) {
            throw new IllegalArgumentException("LLM 요청의 프롬프트, 응답 스키마와 출력 한도를 설정해야 합니다.");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("LLM temperature는 0 이상 2 이하여야 합니다.");
        }
        responseSchema = Map.copyOf(responseSchema);
    }

    public LlmRequest(String prompt, Map<String, Object> responseSchema, int maxOutputTokens) {
        this(prompt, responseSchema, maxOutputTokens, null);
    }

    @Override
    public String toString() {
        return "LlmRequest[maxOutputTokens=" + maxOutputTokens + ", temperature=" + temperature + "]";
    }
}
