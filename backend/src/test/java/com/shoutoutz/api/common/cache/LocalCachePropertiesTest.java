package com.shoutoutz.api.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocalCachePropertiesTest {

    @Test
    @DisplayName("이름으로 캐시 설정을 찾는다")
    void spec_returnsSpecByName() {
        LocalCacheProperties.Spec spec =
                new LocalCacheProperties.Spec(Duration.ofSeconds(5), Duration.ofSeconds(30), 100);
        LocalCacheProperties properties = new LocalCacheProperties(true, Map.of("feed-list", spec));

        assertThat(properties.spec("feed-list")).isEqualTo(spec);
    }

    @Test
    @DisplayName("설정에 없는 캐시 이름이면 예외가 발생한다")
    void spec_throwsWhenNameIsUnknown() {
        LocalCacheProperties properties = new LocalCacheProperties(true, null);

        assertThatThrownBy(() -> properties.spec("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("갱신 시간이 없거나 양수가 아니면 예외가 발생한다")
    void spec_throwsWhenRefreshIsNotPositive() {
        assertThatThrownBy(() -> new LocalCacheProperties.Spec(null, Duration.ofSeconds(30), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LocalCacheProperties.Spec(Duration.ZERO, Duration.ofSeconds(30), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LocalCacheProperties.Spec(Duration.ofSeconds(-1), Duration.ofSeconds(30), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("상한이 갱신 시간보다 길지 않으면 예외가 발생한다")
    void spec_throwsWhenExpireIsNotLongerThanRefresh() {
        assertThatThrownBy(() -> new LocalCacheProperties.Spec(Duration.ofSeconds(60), Duration.ofSeconds(60), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최대 개수가 1보다 작으면 예외가 발생한다")
    void spec_throwsWhenMaximumSizeIsLessThanOne() {
        assertThatThrownBy(() -> new LocalCacheProperties.Spec(Duration.ofSeconds(5), Duration.ofSeconds(30), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
