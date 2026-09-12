package com.shoutoutz.api.media.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class S3PropertiesTest {

    @Test
    void Presigned_URL_만료시간을_Duration으로_변환한다() {
        S3Properties properties = new S3Properties(
                "shoutoutz-media",
                "region-example",
                120
        );

        assertThat(properties.presignedUrlExpiration()).isEqualTo(Duration.ofMinutes(2));
    }
}
