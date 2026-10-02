package com.shoutoutz.api.auth.infrastructure.oauth.github;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GitHubOAuthPropertiesTest {

    @Test
    @DisplayName("GitHub OAuth 외부 통신 타임아웃은 양수여야 한다")
    void rejectsNonPositiveTimeout() {
        assertThatThrownBy(() -> new GitHubOAuthProperties(
                "client-id",
                "client-secret",
                URI.create("http://localhost:8080/login/oauth2/code/github"),
                Duration.ZERO,
                Duration.ofSeconds(5)
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
