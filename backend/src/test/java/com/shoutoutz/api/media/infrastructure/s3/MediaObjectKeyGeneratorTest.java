package com.shoutoutz.api.media.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.shoutoutz.api.media.domain.MediaPurpose;
import org.junit.jupiter.api.Test;

class MediaObjectKeyGeneratorTest {

    private final MediaObjectKeyGenerator keyGenerator = new MediaObjectKeyGenerator();

    @Test
    void 미디어_용도와_UUID로_객체_키를_생성한다() {
        String key = keyGenerator.generate(MediaPurpose.POST_CONTENT);

        assertThat(key).matches("media/post-content/[0-9a-f-]{36}");
    }
}
