package com.shoutoutz.api.auth.presentation.session;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.user.domain.UserRole;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionAccessor {

    private static final String LOGIN_ATTEMPT = "oauthLoginAttempt";
    private static final String AUTHENTICATION = "authenticatedSession";
    private static final String PENDING_OAUTH_IDENTITY = "pendingOAuthIdentity";

    public void saveLoginAttempt(HttpSession session, OAuthLoginAttempt attempt) {
        session.setAttribute(LOGIN_ATTEMPT, attempt);
    }

    public OAuthLoginAttempt consumeLoginAttempt(HttpSession session) {
        if (session == null) {
            throw new IllegalArgumentException("OAuth 로그인 세션이 없습니다.");
        }
        Object value = session.getAttribute(LOGIN_ATTEMPT);
        session.removeAttribute(LOGIN_ATTEMPT);
        if (!(value instanceof OAuthLoginAttempt attempt)) {
            throw new IllegalArgumentException("OAuth 로그인 시도가 없습니다.");
        }
        return attempt;
    }

    public void saveAuthentication(HttpSession session, Long userId, UserRole role) {
        session.setAttribute(AUTHENTICATION, new AuthenticatedSession(userId, role));
        session.removeAttribute(PENDING_OAUTH_IDENTITY);
    }

    public Optional<AuthenticatedSession> findAuthentication(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(AUTHENTICATION);
        if (value instanceof AuthenticatedSession authentication) {
            return Optional.of(authentication);
        }
        return Optional.empty();
    }

    public void savePendingIdentity(HttpSession session, OAuthIdentity identity) {
        session.setAttribute(PENDING_OAUTH_IDENTITY, identity);
        session.removeAttribute(AUTHENTICATION);
    }

    public Optional<OAuthIdentity> findPendingIdentity(HttpSession session) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(PENDING_OAUTH_IDENTITY);
        if (value instanceof OAuthIdentity identity) {
            return Optional.of(identity);
        }
        return Optional.empty();
    }

    public boolean requiresCsrfProtection(HttpSession session) {
        return findAuthentication(session).isPresent()
                || findPendingIdentity(session).isPresent();
    }
}
