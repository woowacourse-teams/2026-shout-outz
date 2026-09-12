package com.shoutoutz.api.media.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.MediaQueryService;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
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
class MediaQueryHttpApiTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-31T00:05:00Z");

    @Mock
    private MediaQueryService mediaQueryService;

    private MediaQueryHttpApi mediaQueryHttpApi;

    @BeforeEach
    void setUp() {
        mediaQueryHttpApi = new MediaQueryHttpApi(mediaQueryService);
    }

    @Test
    void 인증된_사용자의_미디어_조회_요청을_서비스에_전달한다() {
        MediaDownloadResponse expected = response();
        when(mediaQueryService.createDownloadUrl(7L, 10L, MediaVariant.THUMBNAIL)).thenReturn(expected);

        ResponseEntity<MediaDownloadResponse> response = mediaQueryHttpApi.createDownloadUrl(
                10L,
                MediaVariant.THUMBNAIL,
                principal("7")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(mediaQueryService).createDownloadUrl(7L, 10L, MediaVariant.THUMBNAIL);
    }

    @Test
    void 비로그인_조회_요청은_null_사용자_ID로_서비스에_전달한다() {
        MediaDownloadResponse expected = response();
        when(mediaQueryService.createDownloadUrl(null, 10L, MediaVariant.DISPLAY)).thenReturn(expected);

        ResponseEntity<MediaDownloadResponse> response = mediaQueryHttpApi.createDownloadUrl(
                10L,
                MediaVariant.DISPLAY,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(mediaQueryService).createDownloadUrl(null, 10L, MediaVariant.DISPLAY);
    }

    @Test
    void 숫자가_아닌_인증_주체는_조회_서비스를_호출하지_않고_401을_반환한다() {
        Throwable thrown = catchThrowable(() -> mediaQueryHttpApi.createDownloadUrl(
                10L,
                MediaVariant.DISPLAY,
                principal("user-7")
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(mediaQueryService);
    }

    @Test
    void 영보다_작거나_같은_인증_주체는_조회_서비스를_호출하지_않고_401을_반환한다() {
        Throwable thrown = catchThrowable(() -> mediaQueryHttpApi.createDownloadUrl(
                10L,
                MediaVariant.DISPLAY,
                principal("0")
        ));

        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) thrown).getStatusCode().value())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verifyNoInteractions(mediaQueryService);
    }

    private MediaDownloadResponse response() {
        return new MediaDownloadResponse(
                10L,
                MediaVariant.DISPLAY,
                URI.create("https://s3.example.com/download"),
                EXPIRES_AT,
                "image/webp"
        );
    }

    private Principal principal(String name) {
        return () -> name;
    }
}
