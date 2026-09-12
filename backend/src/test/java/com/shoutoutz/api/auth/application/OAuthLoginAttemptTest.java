package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthLoginAttemptTest {

    @Test
    @DisplayName("OAuth 로그인 시도에 필요한 state와 PKCE 값을 생성한다")
    void createsStateAndPkceValues() {
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();

        assertThat(attempt.state()).hasSize(43);
        assertThat(attempt.codeVerifier()).hasSize(86);
        assertThat(attempt.codeChallenge()).isEqualTo(createCodeChallenge(
                attempt.codeVerifier()
        ));
        assertThat(attempt.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("OAuth 로그인 시도마다 새로운 보안 값을 생성한다")
    void createsNewSecurityValuesForEachAttempt() {
        OAuthLoginAttempt firstAttempt = OAuthLoginAttempt.create();
        OAuthLoginAttempt secondAttempt = OAuthLoginAttempt.create();

        assertThat(firstAttempt.state()).isNotEqualTo(secondAttempt.state());
        assertThat(firstAttempt.codeVerifier()).isNotEqualTo(secondAttempt.codeVerifier());
    }

    @Test
    @DisplayName("Callback state가 로그인 시도의 state와 다르면 거부한다")
    void rejectsMismatchedState() {
        OAuthLoginAttempt attempt = new OAuthLoginAttempt(
                "expected-state",
                "code-verifier",
                Instant.parse("2026-09-03T00:00:00Z")
        );

        assertThatThrownBy(() -> attempt.validateCallback(
                "other-state",
                Instant.parse("2026-09-03T00:01:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OAuth 로그인 시도가 생성된 지 5분을 초과하면 거부한다")
    void rejectsExpiredAttempt() {
        OAuthLoginAttempt attempt = new OAuthLoginAttempt(
                "state",
                "code-verifier",
                Instant.parse("2026-09-03T00:00:00Z")
        );

        assertThatThrownBy(() -> attempt.validateCallback(
                "state",
                Instant.parse("2026-09-03T00:05:01Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private String createCodeChallenge(String codeVerifier) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
