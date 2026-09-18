package com.shoutoutz.api.media.infrastructure.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.infrastructure.config.CloudFrontProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaPublicUrlResolverTest {

    @Mock
    private S3MediaStorage s3MediaStorage;

    private MediaPublicUrlResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MediaPublicUrlResolver(
                new CloudFrontProperties("https://cdn.example.com"),
                s3MediaStorage
        );
    }

    @Test
    void key_prefix를_포함한_실제_객체_경로로_공개_URL을_만든다() {
        when(s3MediaStorage.actualKey("media/feed-content/object-id/display"))
                .thenReturn("dev/feed-content/object-id/display");

        assertThat(resolver.resolve("media/feed-content/object-id/display"))
                .hasToString("https://cdn.example.com/dev/feed-content/object-id/display");
    }
}
