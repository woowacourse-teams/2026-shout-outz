package com.shoutoutz.api.media.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.EntityNotFoundException;
import com.shoutoutz.api.common.exception.custom.ForbiddenException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaMetadataRepository;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.presentation.dto.response.MediaStatusResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaStatusQueryServiceTest {

    private static final long USER_ID = 7L;
    private static final long MEDIA_ID = 10L;
    private static final Instant NOW = Instant.parse("2026-09-29T00:00:00Z");

    @Mock
    private MediaMetadataRepository mediaMetadataRepository;

    private MediaStatusQueryService mediaStatusQueryService;

    @BeforeEach
    void setUp() {
        mediaStatusQueryService = new MediaStatusQueryService(mediaMetadataRepository);
    }

    @ParameterizedTest
    @EnumSource(MediaStatus.class)
    void 업로더는_모든_미디어_상태를_조회할_수_있다(MediaStatus status) {
        when(mediaMetadataRepository.findById(MEDIA_ID))
                .thenReturn(Optional.of(media(USER_ID, status)));

        MediaStatusResponse response = mediaStatusQueryService.getStatus(USER_ID, MEDIA_ID);

        assertThat(response.mediaId()).isEqualTo(MEDIA_ID);
        assertThat(response.status()).isEqualTo(status);
    }

    @Test
    void 미디어_ID가_0_이하면_조회하지_않고_400_오류를_낸다() {
        assertThatThrownBy(() -> mediaStatusQueryService.getStatus(USER_ID, 0L))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(mediaMetadataRepository);
    }

    @Test
    void 존재하지_않는_미디어는_404_오류를_낸다() {
        when(mediaMetadataRepository.findById(MEDIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaStatusQueryService.getStatus(USER_ID, MEDIA_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void 다른_사용자의_미디어_상태는_조회할_수_없다() {
        when(mediaMetadataRepository.findById(MEDIA_ID))
                .thenReturn(Optional.of(media(99L, MediaStatus.READY)));

        assertThatThrownBy(() -> mediaStatusQueryService.getStatus(USER_ID, MEDIA_ID))
                .isInstanceOf(ForbiddenException.class);
    }

    private static MediaMetadata media(long uploadedBy, MediaStatus status) {
        return MediaMetadata.reconstitute(
                MEDIA_ID,
                uploadedBy,
                MediaPurpose.USER_AVATAR,
                "media/user-avatar/object-id",
                "avatar.webp",
                "image/webp",
                1024L,
                status,
                NOW.plusSeconds(300),
                status == MediaStatus.FAILED ? "이미지 처리 실패" : null,
                status == MediaStatus.PENDING_UPLOAD ? null : NOW,
                NOW,
                NOW
        );
    }
}
