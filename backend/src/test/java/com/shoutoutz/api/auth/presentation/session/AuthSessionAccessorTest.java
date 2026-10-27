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
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
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

    private final AuthSessionAccessor authSessionAccessor = new AuthSessionAccessor();

    @Test
    @DisplayName("Callback이 시도를 소비한 뒤에도 새 로그인 시작을 차단한다")
    void rejectsStartWhileCallbackIsProcessing() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, attempt);
        authSessionAccessor.consumeLoginAttempt(session, attempt.state());

        assertThatThrownBy(() -> authSessionAccessor.saveLoginAttempt(
                session,
                OAuthLoginAttempt.create()
        )).isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("처리 중인 Callback을 다시 요청하면 중복 처리 오류로 거부한다")
    void rejectsDuplicateCallbackWhileProcessing() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, attempt);
        authSessionAccessor.consumeLoginAttempt(session, attempt.state());

        assertThatThrownBy(() -> authSessionAccessor.consumeLoginAttempt(session, attempt.state()))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(
                        ((ConflictException) exception).getErrorCode().name()
                ).isEqualTo("OAUTH_LOGIN_IN_PROGRESS"));
    }

    @Test
    @DisplayName("진행 중인 OAuth 로그인 시도는 새 시작 요청이 덮어쓰지 않는다")
    void rejectsDuplicateStartAndPreservesOriginalAttempt() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt original = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, original);

        assertThatThrownBy(() -> authSessionAccessor.saveLoginAttempt(
                session,
                OAuthLoginAttempt.create()
        ))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(
                        ((ConflictException) exception).getErrorCode().name()
                ).isEqualTo("OAUTH_LOGIN_IN_PROGRESS"));
        assertThat(authSessionAccessor.consumeLoginAttempt(session, original.state()))
                .isEqualTo(original);
    }

    @Test
    @DisplayName("만료된 OAuth 로그인 시도 대신 새 로그인을 시작할 수 있다")
    void replacesExpiredAttempt() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt expired = new OAuthLoginAttempt(
                "expired-state",
                "expired-verifier",
                Instant.parse("2026-09-03T00:00:00Z")
        );
        OAuthLoginAttempt replacement = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, expired);

        authSessionAccessor.saveLoginAttempt(session, replacement);

        assertThat(authSessionAccessor.consumeLoginAttempt(session, replacement.state()))
                .isEqualTo(replacement);
    }

    @Test
    @DisplayName("같은 세션에서 동시에 시작한 OAuth 로그인은 하나만 저장된다")
    void acceptsOnlyOneConcurrentStart() throws Exception {
        MockHttpSession session = new MockHttpSession();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<OAuthLoginAttempt> task = () -> {
            OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                authSessionAccessor.saveLoginAttempt(session, attempt);
                return attempt;
            } catch (ConflictException exception) {
                assertThat(exception.getErrorCode().name()).isEqualTo("OAUTH_LOGIN_IN_PROGRESS");
                return null;
            }
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(task);
            var second = executor.submit(task);
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            var accepted = Arrays.asList(
                            first.get(5, TimeUnit.SECONDS),
                            second.get(5, TimeUnit.SECONDS)
                    ).stream()
                    .filter(attempt -> attempt != null)
                    .toList();

            assertThat(accepted).hasSize(1);
            OAuthLoginAttempt stored = accepted.getFirst();
            assertThat(authSessionAccessor.consumeLoginAttempt(session, stored.state()))
                    .isEqualTo(stored);
        }
    }

    @Test
    @DisplayName("OAuth 로그인 시도는 세션에서 한 번만 가져올 수 있다")
    void consumesLoginAttemptOnce() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = new OAuthLoginAttempt(
                "state",
                "code-verifier",
                Instant.now()
        );
        authSessionAccessor.saveLoginAttempt(session, attempt);

        OAuthLoginAttempt consumed = authSessionAccessor.consumeLoginAttempt(session, "state");

        assertThat(consumed).isEqualTo(attempt);
        assertThatThrownBy(() -> authSessionAccessor.consumeLoginAttempt(session, "state"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("Callback state가 일치하지 않으면 OAuth 로그인 시도를 유지한다")
    void preservesLoginAttemptWhenStateDoesNotMatch() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = new OAuthLoginAttempt(
                "expected-state",
                "code-verifier",
                Instant.now()
        );
        authSessionAccessor.saveLoginAttempt(session, attempt);

        assertThatThrownBy(() -> authSessionAccessor.consumeLoginAttempt(
                session,
                "other-state"
        )).isInstanceOf(BadRequestException.class);

        assertThat(authSessionAccessor.consumeLoginAttempt(session, "expected-state"))
                .isEqualTo(attempt);
    }

    @Test
    @DisplayName("로그인 시도를 종료하면 새 로그인을 시작할 수 있다")
    void allowsRestartAfterAttemptFinishes() {
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, attempt);
        authSessionAccessor.consumeLoginAttempt(session, attempt.state());

        authSessionAccessor.finishLoginAttempt(session, attempt);

        OAuthLoginAttempt replacement = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, replacement);
        assertThat(authSessionAccessor.consumeLoginAttempt(session, replacement.state()))
                .isEqualTo(replacement);
    }

    @Test
    @DisplayName("만료된 Callback은 만료 오류로 거부하고 새 로그인을 허용한다")
    void rejectsExpiredCallbackAndAllowsRestart() {
        Instant createdAt = Instant.parse("2026-10-27T00:00:00Z");
        AtomicReference<Instant> now = new AtomicReference<>(createdAt);
        AuthSessionAccessor accessor = accessorWithClock(now);
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt attempt = new OAuthLoginAttempt("state", "verifier", createdAt);
        accessor.saveLoginAttempt(session, attempt);
        now.set(createdAt.plusSeconds(301));

        assertThatThrownBy(() -> accessor.consumeLoginAttempt(session, attempt.state()))
                .isInstanceOf(BadRequestException.class)
                .satisfies(exception -> assertThat(((BadRequestException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.OAUTH_LOGIN_ATTEMPT_EXPIRED));

        OAuthLoginAttempt replacement = new OAuthLoginAttempt("new-state", "verifier", now.get());
        accessor.saveLoginAttempt(session, replacement);
        assertThat(accessor.consumeLoginAttempt(session, replacement.state())).isEqualTo(replacement);
    }

    @Test
    @DisplayName("만료된 이전 Callback의 정리는 새 로그인 시도를 제거하지 않는다")
    void preservesReplacementWhenOldCallbackFinishes() {
        Instant createdAt = Instant.parse("2026-10-27T00:00:00Z");
        AtomicReference<Instant> now = new AtomicReference<>(createdAt);
        AuthSessionAccessor accessor = accessorWithClock(now);
        MockHttpSession session = new MockHttpSession();
        OAuthLoginAttempt original = new OAuthLoginAttempt("old-state", "verifier", createdAt);
        accessor.saveLoginAttempt(session, original);
        accessor.consumeLoginAttempt(session, original.state());
        now.set(createdAt.plusSeconds(301));
        OAuthLoginAttempt replacement = new OAuthLoginAttempt("new-state", "verifier", now.get());
        accessor.saveLoginAttempt(session, replacement);

        accessor.finishLoginAttempt(session, original);

        assertThat(accessor.consumeLoginAttempt(session, replacement.state())).isEqualTo(replacement);
    }

    @Test
    @DisplayName("늦은 이전 Callback은 세션을 회전하거나 새 인증 정보를 저장하지 않는다")
    void rejectsOldCompletionWithoutChangingSession() {
        Instant createdAt = Instant.parse("2026-10-27T00:00:00Z");
        AtomicReference<Instant> now = new AtomicReference<>(createdAt);
        AuthSessionAccessor accessor = accessorWithClock(now);
        AuthSessionManager manager = new AuthSessionManager(accessor);
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        String sessionId = session.getId();
        OAuthLoginAttempt original = new OAuthLoginAttempt("old-state", "verifier", createdAt);
        accessor.saveLoginAttempt(session, original);
        accessor.consumeLoginAttempt(session, original.state());
        now.set(createdAt.plusSeconds(301));
        OAuthLoginAttempt replacement = new OAuthLoginAttempt("new-state", "verifier", now.get());
        accessor.saveLoginAttempt(session, replacement);

        assertThatThrownBy(() -> manager.completeOAuthLogin(
                request,
                original,
                OAuthLoginCallbackResult.authenticated(1L, UserRole.USER)
        )).isInstanceOf(BadRequestException.class);

        assertThat(session.getId()).isEqualTo(sessionId);
        assertThat(accessor.findAuthentication(session)).isEmpty();
        assertThat(accessor.consumeLoginAttempt(session, replacement.state())).isEqualTo(replacement);
    }

    @Test
    @DisplayName("처리 중 만료된 Callback은 인증 세션을 만들지 않는다")
    void rejectsCompletionWhenProcessingAttemptExpires() {
        Instant createdAt = Instant.parse("2026-10-27T00:00:00Z");
        AtomicReference<Instant> now = new AtomicReference<>(createdAt);
        AuthSessionAccessor accessor = accessorWithClock(now);
        AuthSessionManager manager = new AuthSessionManager(accessor);
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpSession session = request.getSession();
        String sessionId = session.getId();
        OAuthLoginAttempt attempt = new OAuthLoginAttempt("state", "verifier", createdAt);
        accessor.saveLoginAttempt(session, attempt);
        accessor.consumeLoginAttempt(session, attempt.state());
        now.set(createdAt.plusSeconds(301));

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
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
        authSessionAccessor.saveLoginAttempt(session, attempt);
        authSessionAccessor.consumeLoginAttempt(session, attempt.state());
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
