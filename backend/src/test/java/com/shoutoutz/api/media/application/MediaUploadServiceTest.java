package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.domain.MediaUploadPolicy;
import com.shoutoutz.api.media.infrastructure.s3.MediaObjectKeyGenerator;
import com.shoutoutz.api.media.infrastructure.s3.PresignedUpload;
import com.shoutoutz.api.media.infrastructure.s3.S3MediaStorage;
import com.shoutoutz.api.media.presentation.dto.request.MediaUploadStartRequest;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadStartResponse;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-31T00:00:00Z");

    @Mock
    private MediaUploadAuthorizer mediaUploadAuthorizer;

    @Mock
    private MediaUploadPolicy mediaUploadPolicy;

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    @Mock
    private MediaObjectKeyGenerator mediaObjectKeyGenerator;

    @Mock
    private S3MediaStorage s3MediaStorage;

    private MediaUploadService mediaUploadService;

    @BeforeEach
    void setUp() {
        mediaUploadService = new MediaUploadService(
                mediaUploadAuthorizer,
                mediaUploadPolicy,
                mediaMetadataRepository,
                mediaObjectKeyGenerator,
                s3MediaStorage
        );
    }

    @Test
    void 권한과_정책을_확인한_뒤_Pending_레코드와_업로드_URL을_반환한다() {
        MediaUploadStartRequest request = request();
        String s3Key = "media/post-content/generated-id";
        MediaMetadata savedMetadata = savedMetadata(s3Key);
        PresignedUpload presignedUpload = new PresignedUpload(
                s3Key,
                URI.create("https://s3.example.com/upload"),
                NOW.plus(5, ChronoUnit.MINUTES),
                "image/webp"
        );

        when(mediaObjectKeyGenerator.generate(MediaPurpose.POST_CONTENT)).thenReturn(s3Key);
        when(s3MediaStorage.presignedUrlExpiration()).thenReturn(Duration.ofMinutes(5));
        when(mediaMetadataRepository.save(any(MediaMetadata.class))).thenReturn(savedMetadata);
        when(s3MediaStorage.createPresignedUpload(s3Key, "image/webp")).thenReturn(presignedUpload);

        MediaUploadStartResponse response = mediaUploadService.startUpload(7L, request);

        verify(mediaUploadAuthorizer).authorize(7L, MediaPurpose.POST_CONTENT, 42L);
        verify(mediaUploadPolicy).validateImage("image/webp", 1024L);

        ArgumentCaptor<MediaMetadata> captor = ArgumentCaptor.forClass(MediaMetadata.class);
        verify(mediaMetadataRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(MediaStatus.PENDING_UPLOAD);
        assertThat(captor.getValue().getS3Key()).isEqualTo(s3Key);
        assertThat(captor.getValue().getMimeType()).isEqualTo("image/webp");
        assertThat(captor.getValue().getSizeBytes()).isEqualTo(1024L);

        assertThat(response.mediaId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(MediaStatus.PENDING_UPLOAD);
        assertThat(response.uploadUrl()).hasToString("https://s3.example.com/upload");
        assertThat(response.contentType()).isEqualTo("image/webp");
    }

    @Test
    void 권한이_없으면_레코드와_URL을_생성하지_않는다() {
        MediaUploadStartRequest request = request();
        doThrow(new MediaUploadForbiddenException("미디어 업로드 권한이 없습니다."))
                .when(mediaUploadAuthorizer)
                .authorize(7L, MediaPurpose.POST_CONTENT, 42L);

        assertThatThrownBy(() -> mediaUploadService.startUpload(7L, request))
                .isInstanceOf(MediaUploadForbiddenException.class);

        verifyNoInteractions(mediaUploadPolicy, mediaMetadataRepository, mediaObjectKeyGenerator, s3MediaStorage);
    }

    @Test
    void 포맷이나_용량이_정책에_맞지_않으면_레코드와_URL을_생성하지_않는다() {
        MediaUploadStartRequest request = request();
        doThrow(new IllegalArgumentException("지원하지 않는 이미지 Content-Type입니다."))
                .when(mediaUploadPolicy)
                .validateImage("image/webp", 1024L);

        assertThatThrownBy(() -> mediaUploadService.startUpload(7L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(mediaUploadAuthorizer).authorize(7L, MediaPurpose.POST_CONTENT, 42L);
        verifyNoInteractions(mediaMetadataRepository, mediaObjectKeyGenerator, s3MediaStorage);
    }

    private MediaUploadStartRequest request() {
        return new MediaUploadStartRequest(
                MediaPurpose.POST_CONTENT,
                42L,
                "post-image.webp",
                "image/webp",
                1024L
        );
    }

    private MediaMetadata savedMetadata(String s3Key) {
        return MediaMetadata.reconstitute(
                10L,
                MediaPurpose.POST_CONTENT,
                s3Key,
                "post-image.webp",
                "image/webp",
                1024L,
                MediaStatus.PENDING_UPLOAD,
                NOW.plus(5, ChronoUnit.MINUTES),
                null,
                null,
                NOW,
                NOW
        );
    }
}
