package com.shoutoutz.api.common.llm;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(Gemini gemini, OpenAi openai, Bedrock bedrock) {
    public LlmProperties {
        gemini = gemini == null ? new Gemini(null, null, null, null) : gemini;
        openai = openai == null ? new OpenAi(null, null, null, null, null) : openai;
        bedrock = bedrock == null ? new Bedrock(null, null, null, null, null) : bedrock;
    }

    /** 제공자 한 번 호출에 쓰는 최대 시간(연결 + 읽기). */
    public Duration timeout(LlmProvider provider) {
        return switch (provider) {
            case GEMINI -> gemini.connectTimeout().plus(gemini.readTimeout());
            case OPENAI -> openai.connectTimeout().plus(openai.readTimeout());
            case BEDROCK_GPT, BEDROCK_CLAUDE -> bedrock.connectTimeout().plus(bedrock.readTimeout());
        };
    }

    /** API 키가 필요한 제공자인데 키가 없으면 false. Bedrock 은 IAM 역할로 인증해 키가 필요 없다. */
    public boolean hasCredentials(LlmProvider provider) {
        return switch (provider) {
            case GEMINI -> gemini.apiKey() != null && !gemini.apiKey().isBlank();
            case OPENAI -> openai.apiKey() != null && !openai.apiKey().isBlank();
            case BEDROCK_GPT, BEDROCK_CLAUDE -> true;
        };
    }

    public record Bedrock(@DefaultValue("ap-northeast-2") String region,
            @DefaultValue("global.openai.gpt-6.1-sol") String gptModel,
            @DefaultValue("global.anthropic.claude-sonnet-5-5") String claudeModel,
            @DefaultValue("5s") Duration connectTimeout, @DefaultValue("45s") Duration readTimeout) {
        public Bedrock {
            region = defaultModel(region, "ap-northeast-2");
            gptModel = defaultModel(gptModel, "global.openai.gpt-6.1-sol");
            claudeModel = defaultModel(claudeModel, "global.anthropic.claude-sonnet-5-5");
            connectTimeout = timeout(connectTimeout, Duration.ofSeconds(5));
            readTimeout = timeout(readTimeout, Duration.ofSeconds(45));
        }
    }

    public record Gemini(String apiKey, @DefaultValue("gemini-3.8-flash") String model,
            @DefaultValue("5s") Duration connectTimeout, @DefaultValue("45s") Duration readTimeout) {
        public Gemini {
            model = defaultModel(model, "gemini-3.8-flash");
            connectTimeout = timeout(connectTimeout, Duration.ofSeconds(5));
            readTimeout = timeout(readTimeout, Duration.ofSeconds(45));
        }

        @Override
        public String toString() {
            return "Gemini[model=" + model + "]";
        }
    }

    public record OpenAi(String apiKey, @DefaultValue("gpt-6-luna") String model,
            @DefaultValue("5s") Duration connectTimeout, @DefaultValue("45s") Duration readTimeout,
            @DefaultValue("none") String reasoningEffort) {
        public OpenAi {
            model = defaultModel(model, "gpt-6-luna");
            connectTimeout = timeout(connectTimeout, Duration.ofSeconds(5));
            readTimeout = timeout(readTimeout, Duration.ofSeconds(45));
            reasoningEffort = reasoningEffort == null || reasoningEffort.isBlank() ? "none" : reasoningEffort;
            if (!Set.of("none", "low", "medium", "high", "xhigh", "max").contains(reasoningEffort)) {
                throw new IllegalArgumentException("OpenAI reasoning-effort 설정이 올바르지 않습니다.");
            }
        }

        @Override
        public String toString() {
            return "OpenAi[model=" + model + "]";
        }
    }

    private static String defaultModel(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Duration timeout(Duration value, Duration fallback) {
        Duration result = value == null ? fallback : value;
        if (result.isNegative() || result.isZero()) {
            throw new IllegalArgumentException("LLM 연결 및 읽기 제한 시간은 양수여야 합니다.");
        }
        return result;
    }
}
