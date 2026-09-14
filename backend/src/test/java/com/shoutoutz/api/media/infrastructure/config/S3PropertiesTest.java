package com.shoutoutz.api.media.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class S3PropertiesTest {

    @Test
    void Presigned_URL_만료시간을_Duration으로_변환한다() {
        S3Properties properties = new S3Properties(
                "test-bucket",
                "test-region",
                "test-prefix/",
                120
        );

        assertThat(properties.keyPrefix()).isEqualTo("test-prefix/");
        assertThat(properties.presignedUrlExpiration()).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void S3_key_prefix가_슬래시로_끝나지_않으면_거부한다() {
        assertThatThrownBy(() -> new S3Properties(
                "test-bucket",
                "test-region",
                "test-prefix",
                120
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("S3 key prefix는 앞에 /가 없고 /로 끝나는 유효한 prefix여야 합니다.");
    }
}
