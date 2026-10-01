package com.shoutoutz.api.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthLoginPropertiesTest {

    private static final URI DEFAULT_COMPLETION_URI = URI.create("https://shout-ou.tz");
    private static final URI VERCEL_COMPLETION_URI =
            URI.create("https://wooteco-career-qa.vercel.app");

    @Test
    @DisplayName("기본 완료 URI는 허용 목록에 자동으로 포함된다")
    void includesDefaultCompletionUri() {
        OAuthLoginProperties properties = new OAuthLoginProperties(
                DEFAULT_COMPLETION_URI,
                List.of(VERCEL_COMPLETION_URI)
        );

        assertThat(properties.resolveCompletionUri(null))
                .isEqualTo(DEFAULT_COMPLETION_URI);
        assertThat(properties.resolveCompletionUri(DEFAULT_COMPLETION_URI))
                .isEqualTo(DEFAULT_COMPLETION_URI);
    }

    @Test
    @DisplayName("허용 목록에 있는 프론트엔드 완료 URI를 사용할 수 있다")
    void resolvesAllowedCompletionUri() {
        OAuthLoginProperties properties = new OAuthLoginProperties(
                DEFAULT_COMPLETION_URI,
                List.of(VERCEL_COMPLETION_URI)
        );

        assertThat(properties.resolveCompletionUri(VERCEL_COMPLETION_URI))
                .isEqualTo(VERCEL_COMPLETION_URI);
    }

    @Test
    @DisplayName("허용 목록에 없는 프론트엔드 완료 URI를 거부한다")
    void rejectsUnallowedCompletionUri() {
        OAuthLoginProperties properties = new OAuthLoginProperties(
                DEFAULT_COMPLETION_URI,
                List.of(VERCEL_COMPLETION_URI)
        );

        assertThatThrownBy(() -> properties.resolveCompletionUri(
                URI.create("https://evil.example.com")
        ))
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> assertThat(
                        ((BadRequestException) exception).getErrorCode().name()
                ).isEqualTo("OAUTH_COMPLETION_URI_NOT_ALLOWED"));
    }
}
