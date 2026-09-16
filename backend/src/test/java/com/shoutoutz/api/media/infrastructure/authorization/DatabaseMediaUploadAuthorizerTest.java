package com.shoutoutz.api.media.infrastructure.authorization;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.shoutoutz.api.media.application.exception.MediaUploadForbiddenException;
import com.shoutoutz.api.media.domain.MediaPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class DatabaseMediaUploadAuthorizerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DatabaseMediaUploadAuthorizer authorizer;

    @BeforeEach
    void setUp() {
        authorizer = new DatabaseMediaUploadAuthorizer(jdbcTemplate);
    }

    @Test
    void 활성_관리자는_targetId_없이_홈_배너_미디어를_업로드할_수_있다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(7L)))
                .thenReturn(true);

        authorizer.authorize(7L, MediaPurpose.HOME_BANNER, null);

        verify(jdbcTemplate).queryForObject(anyString(), eq(Boolean.class), eq(7L));
    }

    @Test
    void 관리자가_아니면_홈_배너_미디어를_업로드할_수_없다() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), eq(7L)))
                .thenReturn(false);

        assertThatThrownBy(() -> authorizer.authorize(7L, MediaPurpose.HOME_BANNER, null))
                .isInstanceOf(MediaUploadForbiddenException.class);
    }

    @Test
    void 홈_배너에_targetId가_있으면_DB를_조회하지_않고_거부한다() {
        assertThatThrownBy(() -> authorizer.authorize(7L, MediaPurpose.HOME_BANNER, 1L))
                .isInstanceOf(MediaUploadForbiddenException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    void 기존_목적에_targetId가_없으면_DB를_조회하지_않고_거부한다() {
        assertThatThrownBy(() -> authorizer.authorize(7L, MediaPurpose.FEED_CONTENT, null))
                .isInstanceOf(MediaUploadForbiddenException.class);

        verifyNoInteractions(jdbcTemplate);
    }
}
