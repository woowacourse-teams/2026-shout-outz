package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.MediaPublicUrlResolver;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaUrlResolverTest {

    private static final Instant NOW = Instant.parse("2099-09-01T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaObjectKeyGenerator mediaObjectKeyGenerator;

    @Mock
    private MediaPublicUrlResolver mediaPublicUrlResolver;

    private MediaUrlResolver mediaUrlResolver;

    @BeforeEach
    void setUp() {
        mediaUrlResolver = new MediaUrlResolver(
                mediaMetadataRepository,
                mediaObjectKeyGenerator,
                mediaPublicUrlResolver
        );
    }

    @Test
    void 미디어_ID를_한번에_조회해_READY_미디어만_URL로_변환한다() {
        MediaMetadata ready = metadata(10L, MediaStatus.READY);
        MediaMetadata processing = metadata(11L, MediaStatus.PROCESSING);
        when(mediaMetadataRepository.findAllByIds(Set.of(10L, 11L)))
                .thenReturn(List.of(ready, processing));
        when(mediaObjectKeyGenerator.generateVariant(ready.getS3Key(), MediaVariant.DISPLAY))
                .thenReturn(ready.getS3Key() + "/display");
        when(mediaPublicUrlResolver.resolve(ready.getS3Key() + "/display"))
                .thenReturn(URI.create("https://cdn.example.com/display"));

        Map<Long, URI> urls = mediaUrlResolver.resolveAll(List.of(10L, 11L, 10L));

        assertThat(urls).containsOnlyKeys(10L);
        assertThat(urls.get(10L)).hasToString("https://cdn.example.com/display");
        verify(mediaMetadataRepository).findAllByIds(Set.of(10L, 11L));
    }

    @Test
    void 요청한_변형본을_공개_URL로_변환한다() {
        MediaMetadata ready = metadata(10L, MediaStatus.READY);
        when(mediaMetadataRepository.findAllByIds(Set.of(10L))).thenReturn(List.of(ready));
        when(mediaObjectKeyGenerator.generateVariant(ready.getS3Key(), MediaVariant.THUMBNAIL))
                .thenReturn(ready.getS3Key() + "/thumbnail");
        when(mediaPublicUrlResolver.resolve(ready.getS3Key() + "/thumbnail"))
                .thenReturn(URI.create("https://cdn.example.com/thumbnail"));

        Map<Long, URI> urls = mediaUrlResolver.resolveAll(List.of(10L), MediaVariant.THUMBNAIL);

        assertThat(urls.get(10L)).hasToString("https://cdn.example.com/thumbnail");
        verify(mediaObjectKeyGenerator).generateVariant(ready.getS3Key(), MediaVariant.THUMBNAIL);
    }

    @Test
    void READY_미디어를_조회_없이_공개_URL로_변환한다() {
        MediaMetadata ready = metadata(10L, MediaStatus.READY);
        when(mediaObjectKeyGenerator.generateVariant(ready.getS3Key(), MediaVariant.DISPLAY))
                .thenReturn(ready.getS3Key() + "/display");
        when(mediaPublicUrlResolver.resolve(ready.getS3Key() + "/display"))
                .thenReturn(URI.create("https://cdn.example.com/display"));

        URI url = mediaUrlResolver.resolve(ready, MediaVariant.DISPLAY);

        assertThat(url).hasToString("https://cdn.example.com/display");
    }

    @Test
    void 프로젝트_본문의_media_참조를_공개_URL로_치환한다() {
        String description = "![화면](media://10)\n![같은 화면](media://10)";
        Map<Long, URI> urls = Map.of(10L, URI.create("https://cdn.example.com/display"));

        assertThat(mediaUrlResolver.replaceDescriptionReferences(description, urls))
                .isEqualTo("![화면](https://cdn.example.com/display)\n"
                        + "![같은 화면](https://cdn.example.com/display)");
    }

    @Test
    void 프로젝트_본문의_공개_URL을_media_참조로_되돌린다() {
        String description = "![화면](https://cdn.example.com/display)\n"
                + "![같은 화면](https://cdn.example.com/display)";
        Map<Long, URI> urls = Map.of(10L, URI.create("https://cdn.example.com/display"));

        assertThat(mediaUrlResolver.replaceDescriptionUrlsWithReferences(description, urls))
                .isEqualTo("![화면](media://10)\n![같은 화면](media://10)");
    }

    @Test
    void 공개_URL의_인코딩과_query_fragment가_달라도_media_참조로_되돌린다() {
        String description = "![화면](<https://cdn.example.com/media/project-description/object%2D10/display"
                + "?cache=1#section>)";
        Map<Long, URI> urls = Map.of(
                10L,
                URI.create("https://cdn.example.com/media/project-description/object-10/display")
        );

        assertThat(mediaUrlResolver.replaceDescriptionUrlsWithReferences(description, urls))
                .isEqualTo("![화면](<media://10>)");
    }

    @Test
    void media_참조로_변환되지_않은_이미지_소스를_감지한다() {
        assertThat(mediaUrlResolver.containsUnsupportedDescriptionImageReference(
                "![외부 이미지](https://external.example.com/image.png)"
        )).isTrue();
        assertThat(mediaUrlResolver.containsUnsupportedDescriptionImageReference(
                "![인라인 이미지](data:image/png;base64,abc)"
        )).isTrue();
        assertThat(mediaUrlResolver.containsUnsupportedDescriptionImageReference(
                "![미디어](media://10)"
        )).isFalse();
    }

    private MediaMetadata metadata(long id, MediaStatus status) {
        return MediaMetadata.reconstitute(
                id,
                7L,
                MediaPurpose.PROJECT_DESCRIPTION,
                "media/project-description/object-" + id,
                "image.webp",
                "image/webp",
                1024L,
                status,
                NOW,
                null,
                NOW,
                NOW,
                NOW
        );
    }
}
