package com.shoutoutz.api.auth.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

public record OAuthLoginAttempt(
        String state,
        String codeVerifier,
        Instant createdAt
) {

    private static final int STATE_BYTES = 32;
    private static final int CODE_VERIFIER_BYTES = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static OAuthLoginAttempt create() {
        return new OAuthLoginAttempt(
                generateRandomValue(STATE_BYTES),
                generateRandomValue(CODE_VERIFIER_BYTES),
                Instant.now()
        );
    }

    public String codeChallenge() {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }

    private static String generateRandomValue(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
