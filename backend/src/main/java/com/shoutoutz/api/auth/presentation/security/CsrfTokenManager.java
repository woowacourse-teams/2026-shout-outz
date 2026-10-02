package com.shoutoutz.api.auth.presentation.security;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class CsrfTokenManager {

    private static final String CSRF_TOKEN = "csrfToken";
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public String getOrCreate(HttpSession session) {
        Object value = session.getAttribute(CSRF_TOKEN);
        if (value instanceof String csrfToken) {
            return csrfToken;
        }

        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String csrfToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        session.setAttribute(CSRF_TOKEN, csrfToken);
        return csrfToken;
    }

    boolean matches(HttpSession session, String requestedToken) {
        if (session == null || requestedToken == null) {
            return false;
        }
        Object value = session.getAttribute(CSRF_TOKEN);
        if (!(value instanceof String sessionToken)) {
            return false;
        }

        return MessageDigest.isEqual(
                sessionToken.getBytes(StandardCharsets.UTF_8),
                requestedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
