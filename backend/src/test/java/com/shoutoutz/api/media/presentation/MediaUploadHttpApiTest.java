package com.shoutoutz.api.media.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.MediaUploadCompletionService;
import com.shoutoutz.api.media.application.MediaUploadService;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import com.shoutoutz.api.media.presentation.dto.request.MediaUploadStartRequest;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadCompleteResponse;
import com.shoutoutz.api.media.presentation.dto.response.MediaUploadStartResponse;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MediaUploadHttpApiTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-31T00:05:00Z");

    @Mock
    private MediaUploadService mediaUploadService;

    @Mock
    private MediaUploadCompletionService mediaUploadCompletionService;

    private MediaUploadHttpApi mediaUploadHttpApi;

    @BeforeEach
    void setUp() {
        mediaUploadHttpApi = new MediaUploadHttpApi(mediaUploadService, mediaUploadCompletionService);
    }

    @Test
    void 인증된_사용자의_업로드_시작을_서비스에_전달하고_CREATED를_반환한다() {
        MediaUploadStartRequest request = new MediaUploadStartRequest(
                MediaPurpose.POST_CONTENT,
                42L,
                "post-image.webp",
                "image/webp",
                1024L
        );
        MediaUploadStartResponse expected = new MediaUploadStartResponse(
                10L,
                MediaStatus.PENDING_UPLOAD,
                URI.create("https://s3.example.com/upload"),
                EXPIRES_AT,
                "image/webp"
        );
        when(mediaUploadService.startUpload(7L, request)).thenReturn(expected);

        ResponseEntity<MediaUploadStartResponse> response = mediaUploadHttpApi.startUpload(request, principal("7"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(expected);
        verify(mediaUploadService).startUpload(7L, request);
    }

    @Test
    void 인증된_사용자의_업로드_완료를_서비스에_전달하고_OK를_반환한다() {
        MediaUploadCompleteResponse expected = new MediaUploadCompleteResponse(
                10L,
                MediaStatus.PROCESSING,
                1024L,
                "image/webp",
                EXPIRES_AT
        );
        when(mediaUploadCompletionService.completeUpload(7L, 10L)).thenReturn(expected);

        ResponseEntity<MediaUploadCompleteResponse> response = mediaUploadHttpApi.completeUpload(10L, principal("7"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(mediaUploadCompletionService).completeUpload(7L, 10L);
    }

    @Test
    void 인증_정보가_없으면_업로드_서비스를_호출하지_않고_401을_반환한다() {
        Throwable thrown = catchThrowable(() -> mediaUploadHttpApi.startUpload(request(), null));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(mediaUploadService, mediaUploadCompletionService);
    }

    @Test
    void 숫자가_아닌_인증_주체는_거부한다() {
        Throwable thrown = catchThrowable(() -> mediaUploadHttpApi.startUpload(request(), principal("user-7")));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(mediaUploadService, mediaUploadCompletionService);
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

    private Principal principal(String name) {
        return () -> name;
    }
}
