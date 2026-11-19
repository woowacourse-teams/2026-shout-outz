package com.shoutoutz.api.resourcesearch.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class ResourceSearchPropertiesTest {

    @Test
    void 값을_주지_않으면_운영_사양에서_잰_기본값을_쓴다() {
        ResourceSearchProperties properties = new Binder(new MapConfigurationPropertySource(Map.of()))
                .bindOrCreate("resource.search", ResourceSearchProperties.class);

        assertThat(properties.queryTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(properties.maxConcurrent()).isEqualTo(2);
        assertThat(properties.acquireTimeout()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void 쿼리_제한_시간은_1ms_이상이어야_한다() {
        assertThatThrownBy(() -> new ResourceSearchProperties(Duration.ZERO, 2, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 쿼리_제한_시간은_5초까지_허용한다() {
        ResourceSearchProperties properties = new ResourceSearchProperties(
                Duration.ofSeconds(5), 2, Duration.ofSeconds(1)
        );

        assertThat(properties.queryTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void 쿼리_제한_시간이_5초를_넘으면_제한의_의미가_없어_거절한다() {
        assertThatThrownBy(() -> new ResourceSearchProperties(Duration.ofMillis(5001), 2, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5000ms");
    }

    @Test
    void 동시_실행_상한은_1_이상이어야_한다() {
        assertThatThrownBy(() -> new ResourceSearchProperties(Duration.ofSeconds(3), 0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 자리_대기_시간은_음수일_수_없다() {
        assertThatThrownBy(() -> new ResourceSearchProperties(Duration.ofSeconds(3), 2, Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
