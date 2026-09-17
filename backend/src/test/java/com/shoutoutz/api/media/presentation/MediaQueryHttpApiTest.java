package com.shoutoutz.api.media.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.MediaQueryService;
import com.shoutoutz.api.media.infrastructure.s3.MediaVariant;
import com.shoutoutz.api.media.presentation.dto.response.MediaDownloadResponse;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MediaQueryHttpApiTest {

    @Mock
    private MediaQueryService mediaQueryService;

    private MediaQueryHttpApi mediaQueryHttpApi;

    @BeforeEach
    void setUp() {
        mediaQueryHttpApi = new MediaQueryHttpApi(mediaQueryService);
    }

    @Test
    void 미디어_조회_요청을_공개_URL_조회_서비스에_전달한다() {
        MediaDownloadResponse expected = response();
        when(mediaQueryService.resolvePublicUrl(10L, MediaVariant.THUMBNAIL)).thenReturn(expected);

        ResponseEntity<MediaDownloadResponse> response = mediaQueryHttpApi.resolvePublicUrl(
                10L,
                MediaVariant.THUMBNAIL
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(mediaQueryService).resolvePublicUrl(10L, MediaVariant.THUMBNAIL);
    }

    private MediaDownloadResponse response() {
        return new MediaDownloadResponse(
                MediaVariant.THUMBNAIL,
                URI.create("https://cdn.example.com/thumbnail"),
                "image/webp"
        );
    }
}
