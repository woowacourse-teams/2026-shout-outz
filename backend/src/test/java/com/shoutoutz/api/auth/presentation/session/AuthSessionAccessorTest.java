package com.shoutoutz.api.auth.presentation.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import com.shoutoutz.api.auth.application.command.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import com.shoutoutz.api.common.exception.custom.ConflictException;
import com.shoutoutz.api.user.domain.account.UserRole;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

class AuthSessionAccessorTest {

    private static final URI COMPLETION_URI = URI.create("https://shout-ou.tz");
    private static final Instant CREATED_AT = Instant.parse("2026-10-29T00:00:00Z");
    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();

    @Test
    @DisplayName("Callback 처리 중에는 새 로그인 시작을 차단하고 기존 시도를 보존한다")
    void rejectsStartWhileCallbackIsProcessing() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        authSessionAccessor.beginCallbackProcessing(session, attempt.state());

        assertThatThrownBy(() -> authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_LOGIN_IN_PROGRESS));
        authSessionAccessor.validateProcessingAttempt(session, attempt);
    }

    @Test
    @DisplayName("처리 중인 Callback을 다시 요청하면 중복 처리 오류로 거부한다")
    void rejectsDuplicateCallbackWhileProcessing() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        authSessionAccessor.beginCallbackProcessing(session, attempt.state());

        assertThatThrownBy(() -> authSessionAccessor.beginCallbackProcessing(session, attempt.state()))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_LOGIN_IN_PROGRESS));
    }

    @Test
    @DisplayName("WAITING 재시작은 state와 PKCE, 생성 시각과 최초 완료 주소를 모두 유지한다")
    void resumesWaitingAttemptWithoutChangingIt() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt original = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);

        OAuthLoginAttempt resumed = authSessionAccessor.getOrCreateLoginAttempt(
                session,
                URI.create("http://localhost:5173")
        );

        assertThat(resumed).isSameAs(original);
        assertThat(resumed.completionUri()).isEqualTo(COMPLETION_URI);
        assertThat(authSessionAccessor.beginCallbackProcessing(session, original.state())).isSameAs(original);
    }

    @Test
    @DisplayName("WAITING 재사용은 만료를 연장하지 않고 최초 생성 5분 초과 시 새 시도를 만든다")
    void doesNotExtendExpirationWhenResuming() {
        AtomicReference<Instant> now = new AtomicReference<>(CREATED_AT);
        AuthSessionAccessor accessor = accessorWithClock(now);
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt original = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        now.set(CREATED_AT.plusSeconds(299));
        assertThat(accessor.getOrCreateLoginAttempt(session, COMPLETION_URI)).isSameAs(original);
        now.set(CREATED_AT.plusSeconds(300));
        assertThat(accessor.getOrCreateLoginAttempt(session, COMPLETION_URI)).isSameAs(original);

        now.set(CREATED_AT.plusSeconds(300).plusNanos(1));
        OAuthLoginAttempt replacement = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);

        assertThat(replacement.state()).isNotEqualTo(original.state());
        assertThat(replacement.codeVerifier()).isNotEqualTo(original.codeVerifier());
        assertThat(replacement.createdAt()).isEqualTo(now.get());
        assertThat(accessor.beginCallbackProcessing(session, replacement.state())).isSameAs(replacement);
    }

    @Test
    @DisplayName("동시에 시작한 두 요청은 같은 시도와 최초 완료 주소를 반환한다")
    void sharesAttemptForConcurrentStarts() throws Exception {
        MockHttpSession session = new MockHttpSession();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<OAuthLoginAttempt> task = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(task);
            var second = executor.submit(task);
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            OAuthLoginAttempt stored = first.get(5, TimeUnit.SECONDS);
            assertThat(second.get(5, TimeUnit.SECONDS)).isSameAs(stored);
            assertThat(authSessionAccessor.beginCallbackProcessing(session, stored.state())).isSameAs(stored);
        }
    }

    @Test
    @DisplayName("OAuth 로그인 시도는 한 Callback만 처리할 수 있다")
    void beginsCallbackProcessingOnce() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);

        assertThat(authSessionAccessor.beginCallbackProcessing(session, attempt.state())).isSameAs(attempt);
        assertThatThrownBy(() -> authSessionAccessor.beginCallbackProcessing(session, attempt.state()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Callback state가 일치하지 않으면 OAuth 로그인 시도를 유지한다")
    void preservesLoginAttemptWhenStateDoesNotMatch() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);

        assertThatThrownBy(() -> authSessionAccessor.beginCallbackProcessing(session, "other-state"))
                .isInstanceOf(BadRequestException.class);
        assertThat(authSessionAccessor.beginCallbackProcessing(session, attempt.state())).isSameAs(attempt);
    }

    @Test
    @DisplayName("로그인 시도를 종료하면 새 로그인을 시작할 수 있다")
    void allowsRestartAfterAttemptFinishes() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        authSessionAccessor.beginCallbackProcessing(session, attempt.state());

        authSessionAccessor.finishLoginAttempt(session, attempt);

        OAuthLoginAttempt replacement = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        assertThat(replacement.state()).isNotEqualTo(attempt.state());
        assertThat(authSessionAccessor.beginCallbackProcessing(session, replacement.state())).isSameAs(replacement);
    }

    @Test
    @DisplayName("만료된 Callback은 만료 오류로 거부하고 새 로그인을 허용한다")
    void rejectsExpiredCallbackAndAllowsRestart() {
        AtomicReference<Instant> now = new AtomicReference<>(CREATED_AT);
        AuthSessionAccessor accessor = accessorWithClock(now);
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        now.set(CREATED_AT.plusSeconds(301));

        assertThatThrownBy(() -> accessor.beginCallbackProcessing(session, attempt.state()))
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> assertThat(((BadRequestException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_LOGIN_ATTEMPT_EXPIRED));

        OAuthLoginAttempt replacement = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        assertThat(accessor.beginCallbackProcessing(session, replacement.state())).isSameAs(replacement);
    }

    @Test
    @DisplayName("만료된 이전 Callback의 정리는 새 로그인 시도를 제거하지 않는다")
    void preservesReplacementWhenOldCallbackFinishes() {
        AtomicReference<Instant> now = new AtomicReference<>(CREATED_AT);
        AuthSessionAccessor accessor = accessorWithClock(now);
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt original = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        accessor.beginCallbackProcessing(session, original.state());
        now.set(CREATED_AT.plusSeconds(301));
        OAuthLoginAttempt replacement = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);

        accessor.finishLoginAttempt(session, original);

        assertThat(accessor.beginCallbackProcessing(session, replacement.state())).isSameAs(replacement);
    }

    @Test
    @DisplayName("늦은 이전 Callback은 세션을 회전하거나 새 인증 정보를 저장하지 않는다")
    void rejectsOldCompletionWithoutChangingSession() {
        AtomicReference<Instant> now = new AtomicReference<>(CREATED_AT);
        AuthSessionAccessor accessor = accessorWithClock(now);
        AuthSessionManager manager = new AuthSessionManager(accessor);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        OAuthLoginAttempt original = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        accessor.beginCallbackProcessing(session, original.state());
        now.set(CREATED_AT.plusSeconds(301));
        OAuthLoginAttempt replacement = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);

        assertThatThrownBy(() -> manager.completeOAuthLogin(
                request,
                original,
                OAuthLoginCallbackResult.authenticated(1L, UserRole.USER)
        )).isInstanceOf(BadRequestException.class);

        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(accessor.findAuthentication(session)).isEmpty();
        assertThat(accessor.beginCallbackProcessing(session, replacement.state())).isSameAs(replacement);
    }

    @Test
    @DisplayName("처리 중 만료된 Callback은 인증 세션을 만들지 않는다")
    void rejectsCompletionWhenProcessingAttemptExpires() {
        AtomicReference<Instant> now = new AtomicReference<>(CREATED_AT);
        AuthSessionAccessor accessor = accessorWithClock(now);
        AuthSessionManager manager = new AuthSessionManager(accessor);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        OAuthLoginAttempt attempt = accessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        accessor.beginCallbackProcessing(session, attempt.state());
        now.set(CREATED_AT.plusSeconds(301));

        assertThatThrownBy(() -> manager.completeOAuthLogin(
                request,
                attempt,
                OAuthLoginCallbackResult.authenticated(1L, UserRole.USER)
        ))
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> assertThat(((BadRequestException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_LOGIN_ATTEMPT_EXPIRED));
        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(accessor.findAuthentication(session)).isEmpty();
    }

    @Test
    @DisplayName("외부 통신 중 로그아웃한 세션의 종료 처리는 예외를 던지지 않는다")
    void ignoresInvalidatedSessionDuringCleanup() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = authSessionAccessor.getOrCreateLoginAttempt(session, COMPLETION_URI);
        authSessionAccessor.beginCallbackProcessing(session, attempt.state());
        session.invalidate();

        authSessionAccessor.finishLoginAttempt(session, attempt);
    }

    private AuthSessionAccessor accessorWithClock(AtomicReference<Instant> now) {
        Clock clock = mock(Clock.class);
        given(clock.instant()).willAnswer(invocation -> now.get());
        return new AuthSessionAccessor(clock);
    }

    @Test
    @DisplayName("인증 정보를 저장하면 가입 대기 신원을 제거한다")
    void replacesPendingIdentityWithAuthentication() {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.savePendingIdentity(session, githubIdentity());

        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);

        assertThat(authSessionAccessor.findAuthentication(session)).isPresent();
        assertThat(authSessionAccessor.findPendingIdentity(session)).isEmpty();
    }

    @Test
    @DisplayName("가입 대기 신원을 저장하면 기존 인증 정보를 제거한다")
    void replacesAuthenticationWithPendingIdentity() {
        MockHttpSession session = new MockHttpSession();
        authSessionAccessor.saveAuthentication(session, 1L, UserRole.USER);

        authSessionAccessor.savePendingIdentity(session, githubIdentity());

        assertThat(authSessionAccessor.findAuthentication(session)).isEmpty();
        assertThat(authSessionAccessor.findPendingIdentity(session)).contains(githubIdentity());
    }

    @Test
    @DisplayName("인증 또는 가입 대기 정보가 있는 세션을 보호 대상으로 판단한다")
    void identifiesProtectedSession() {
        MockHttpSession unauthenticated = new MockHttpSession();
        MockHttpSession authenticated = new MockHttpSession();
        MockHttpSession signupPending = new MockHttpSession();
        authSessionAccessor.saveAuthentication(authenticated, 1L, UserRole.USER);
        authSessionAccessor.savePendingIdentity(signupPending, githubIdentity());

        assertThat(authSessionAccessor.requiresCsrfProtection(null)).isFalse();
        assertThat(authSessionAccessor.requiresCsrfProtection(unauthenticated)).isFalse();
        assertThat(authSessionAccessor.requiresCsrfProtection(authenticated)).isTrue();
        assertThat(authSessionAccessor.requiresCsrfProtection(signupPending)).isTrue();
    }

    private OAuthIdentity githubIdentity() {
        return new OAuthIdentity(OAuthProvider.GITHUB, "12345678", null);
    }
}
