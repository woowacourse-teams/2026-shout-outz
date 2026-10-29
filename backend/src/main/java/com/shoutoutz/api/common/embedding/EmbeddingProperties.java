package com.shoutoutz.api.common.embedding;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "resource.embedding")
public record EmbeddingProperties(@DefaultValue("openai") String provider,
        @DefaultValue("text-embedding-3-small") String modelId, @DefaultValue("1024") int dimensions,
        @DefaultValue("true") boolean normalize, String apiKey, @DefaultValue("5s") Duration connectTimeout,
        @DefaultValue("30s") Duration readTimeout) {
    public EmbeddingProperties {
        provider = provider == null || provider.isBlank() ? "openai" : provider;
        if (!"openai".equals(provider)) {
            throw new IllegalArgumentException("임베딩 provider는 openai만 지원합니다.");
        }
        modelId = modelId == null || modelId.isBlank() ? "text-embedding-3-small" : modelId;
        if (dimensions <= 0) {
            throw new IllegalArgumentException("임베딩 차원은 양수여야 합니다.");
        }
        connectTimeout = timeout(connectTimeout, Duration.ofSeconds(5));
        readTimeout = timeout(readTimeout, Duration.ofSeconds(30));
    }

    /** resource_chunks.embedding_model 값. 모델이나 차원이 바뀌면 값도 바뀌어 이전 벡터와 섞이지 않는다. */
    public String label() {
        return modelId + "@" + dimensions;
    }

    @Override
    public String toString() {
        return "EmbeddingProperties[provider=" + provider + ", model=" + label() + ", normalize=" + normalize + "]";
    }

    private static Duration timeout(Duration value, Duration fallback) {
        Duration result = value == null ? fallback : value;
        if (result.isNegative() || result.isZero()) {
            throw new IllegalArgumentException("임베딩 연결 및 읽기 제한 시간은 양수여야 합니다.");
        }
        return result;
    }
}
