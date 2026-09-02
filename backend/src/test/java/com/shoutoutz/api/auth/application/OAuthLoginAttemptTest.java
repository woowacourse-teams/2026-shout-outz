package com.shoutoutz.api.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
