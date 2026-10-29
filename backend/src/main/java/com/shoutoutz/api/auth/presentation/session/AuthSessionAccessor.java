package com.shoutoutz.api.auth.presentation.session;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.user.domain.account.UserRole;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AuthSessionAccessor {

    private static final String LOGIN_ATTEMPT = "oauthLoginAttempt";
    private static final String AUTHENTICATION = "authenticatedSession";
    private static final String PENDING_OAUTH_IDENTITY = "pendingOAuthIdentity";

    private final Clock clock;

    public AuthSessionAccessor() {
        this(Clock.systemUTC());
    }

    AuthSessionAccessor(Clock clock) {
        this.clock = clock;
    }

    public OAuthLoginAttempt getOrCreateLoginAttempt(HttpSession session, URI completionUri) {
        synchronized (session) {
            Instant now = Instant.now(clock);
            Object value = session.getAttribute(LOGIN_ATTEMPT);
            if (value instanceof LoginProgress current
                    && !current.attempt().isExpired(now)) {
                if (current.status() == LoginStatus.PROCESSING) {
                    throw new ConflictException(AuthErrorCode.OAUTH_LOGIN_IN_PROGRESS);
                }
                return current.attempt();
            }
            OAuthLoginAttempt attempt = OAuthLoginAttempt.create(completionUri, now);
            session.setAttribute(LOGIN_ATTEMPT, new LoginProgress(attempt, LoginStatus.WAITING));
            return attempt;
        }
    }

    public OAuthLoginAttempt beginCallbackProcessing(HttpSession session, String callbackState) {
        if (session == null) {
            throw new BadRequestException(AuthErrorCode.OAUTH_LOGIN_SESSION_NOT_FOUND);
        }

        synchronized (session) {
            Object value = session.getAttribute(LOGIN_ATTEMPT);
            if (!(value instanceof LoginProgress progress)) {
                throw new BadRequestException(AuthErrorCode.OAUTH_LOGIN_ATTEMPT_NOT_FOUND);
            }

            OAuthLoginAttempt attempt = progress.attempt();
            attempt.validateCallback(callbackState, Instant.now(clock));
            if (progress.status() == LoginStatus.PROCESSING) {
                throw new ConflictException(AuthErrorCode.OAUTH_LOGIN_IN_PROGRESS);
            }
            session.setAttribute(LOGIN_ATTEMPT, new LoginProgress(attempt, LoginStatus.PROCESSING));
            return attempt;
        }
    }

    void validateProcessingAttempt(HttpSession session, OAuthLoginAttempt attempt) {
        Object value = session.getAttribute(LOGIN_ATTEMPT);
        if (!(value instanceof LoginProgress progress)
                || progress.status() != LoginStatus.PROCESSING
                || !progress.attempt().equals(attempt)) {
            throw new BadRequestException(AuthErrorCode.OAUTH_LOGIN_ATTEMPT_NOT_FOUND);
        }
        attempt.validateExpiration(Instant.now(clock));
    }

    public void finishLoginAttempt(HttpSession session, OAuthLoginAttempt attempt) {
        if (session == null) {
            return;
        }
        synchronized (session) {
            try {
                Object value = session.getAttribute(LOGIN_ATTEMPT);
                if (value instanceof LoginProgress progress && progress.attempt().equals(attempt)) {
                    session.removeAttribute(LOGIN_ATTEMPT);
                }
            } catch (IllegalStateException ignored) {
                // 외부 통신 중 로그아웃으로 세션이 무효화되면 정리할 속성도 없다.
            }
        }
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

    private enum LoginStatus {
        WAITING,
        PROCESSING
    }

    private record LoginProgress(OAuthLoginAttempt attempt, LoginStatus status) {
    }
}
