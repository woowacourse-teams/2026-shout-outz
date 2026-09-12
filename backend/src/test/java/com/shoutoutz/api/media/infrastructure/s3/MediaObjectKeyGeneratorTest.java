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

    @Test
    void 원본_키에서_표시용과_썸네일_키를_결정적으로_생성한다() {
        String sourceKey = "media/post-content/object-id";

        assertThat(keyGenerator.generateVariant(sourceKey, MediaVariant.ORIGINAL))
                .isEqualTo(sourceKey);
        assertThat(keyGenerator.generateVariant(sourceKey, MediaVariant.DISPLAY))
                .isEqualTo(sourceKey + "/display");
        assertThat(keyGenerator.generateVariant(sourceKey, MediaVariant.THUMBNAIL))
                .isEqualTo(sourceKey + "/thumbnail");
    }
}
