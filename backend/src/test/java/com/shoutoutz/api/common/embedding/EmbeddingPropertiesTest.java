package com.shoutoutz.api.common.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class EmbeddingPropertiesTest {

    @Test
    void 제한_시간을_주지_않으면_검색_요청이_오래_붙잡히지_않는_기본값을_쓴다() {
        EmbeddingProperties properties = new EmbeddingProperties("openai", null, 1024, true, "key", null, null);

        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void 설정한_제한_시간을_그대로_쓴다() {
        EmbeddingProperties properties = new EmbeddingProperties(
                "openai", null, 1024, true, "key", Duration.ofMillis(500), Duration.ofSeconds(1)
        );

        assertThat(properties.connectTimeout()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(1));
    }
}
