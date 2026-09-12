package com.shoutoutz.api.media.infrastructure.authorization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.exception.MediaAccessForbiddenException;
import com.shoutoutz.api.media.application.exception.MediaAccessUnauthorizedException;
import com.shoutoutz.api.media.domain.MediaMetadata;
import com.shoutoutz.api.media.domain.MediaPurpose;
import com.shoutoutz.api.media.domain.MediaStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DatabaseMediaAccessAuthorizerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DatabaseMediaAccessAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        authorizer = new DatabaseMediaAccessAuthorizer(jdbcTemplate);
    }

    @Test
    void 삭제되지_않은_포스트에_연결된_미디어는_비로그인_조회가_가능하다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(10L)))
                .thenReturn(true);

        authorizer.authorize(null, metadata());

        verify(jdbcTemplate).queryForObject(anyString(), eq(Boolean.class), eq(10L));
    }

    @Test
    void 포스트에_연결되지_않은_미디어는_업로더만_조회할_수_있다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(10L)))
                .thenReturn(false);

        authorizer.authorize(7L, metadata());

        assertThatThrownBy(() -> authorizer.authorize(8L, metadata()))
                .isInstanceOf(MediaAccessForbiddenException.class);
    }

    @Test
    void 비공개_미디어를_비로그인으로_조회하면_인증을_요구한다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(10L)))
                .thenReturn(false);

        assertThatThrownBy(() -> authorizer.authorize(null, metadata()))
                .isInstanceOf(MediaAccessUnauthorizedException.class);
    }

    private MediaMetadata metadata() {
        Instant now = Instant.parse("2099-09-01T00:00:00Z");
        return MediaMetadata.reconstitute(
                10L,
                7L,
                MediaPurpose.POST_CONTENT,
                "media/post-content/object-id",
                "post-image.webp",
                "image/webp",
                1024L,
                MediaStatus.READY,
                now.plus(5, ChronoUnit.MINUTES),
                null,
                now,
                now,
                now
        );
    }
}
