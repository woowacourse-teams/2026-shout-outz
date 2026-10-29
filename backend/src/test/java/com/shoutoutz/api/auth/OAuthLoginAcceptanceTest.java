package com.shoutoutz.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.presentation.security.AuthenticatedUser;
import com.shoutoutz.api.auth.presentation.security.LoginUser;
import com.shoutoutz.api.user.domain.account.Handle;
import com.shoutoutz.api.user.domain.profile.ProfileDisplayName;
import com.shoutoutz.api.user.domain.account.User;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserProfileRepository;
import com.shoutoutz.api.user.domain.account.UserRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@ActiveProfiles("test")
@Import(OAuthLoginAcceptanceTest.TestAuthHttpApi.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuthLoginAcceptanceTest {

    private static final String GITHUB_AUTHORIZATION_PATH =
            "/oauth2/authorization/github";
    private static final String AUTH_SESSION_PATH = "/api/v1/auth/session";
    private static final String OAUTH_SIGNUP_PATH = "/api/v1/auth/signup";
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";
    private static final String CSRF_TEST_PATH = "/api/v1/projects/csrf-test";
    private static final String AUTHENTICATED_USER_TEST_PATH =
            "/api/v1/projects/authenticated-user-test";

    @LocalServerPort
    private int port;

    @MockitoBean
    private GitHubOAuthIdentityPort gitHubOAuthIdentityPort;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private OAuthAccountRepository oauthAccountRepository;

    @Test
    @DisplayName("GitHub OAuth 로그인에 필요한 값과 세션을 생성하고 GitHub로 이동한다")
    void redirectsToGitHubAuthorizationEndpoint() {
        Response response = requestAuthorization();

        assertThat(response.statusCode()).isEqualTo(302);

        URI location = URI.create(response.header("Location"));
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(location)
                .build()
                .getQueryParams();

        assertThat(response.cookie("JSESSIONID")).isNotBlank();
        assertThat(location.getScheme()).isEqualTo("https");
        assertThat(location.getHost()).isEqualTo("github.com");
        assertThat(location.getPath()).isEqualTo("/login/oauth/authorize");
        assertThat(queryParams.getFirst("client_id")).isEqualTo("test-client-id");
        assertThat(queryParams.getFirst("redirect_uri"))
                .isEqualTo("http://localhost:8080/login/oauth2/code/github");
        assertThat(queryParams.getFirst("state")).isNotBlank();
        assertThat(queryParams.getFirst("code_challenge")).isNotBlank();
        assertThat(queryParams.getFirst("code_challenge_method")).isEqualTo("S256");
    }

    @Test
    @DisplayName("GitHub OAuth 로그인 시도마다 새로운 state와 PKCE challenge를 생성한다")
    void generatesNewStateAndCodeChallengeForEachAttempt() {
        MultiValueMap<String, String> firstQueryParams = authorizationQueryParams(
                requestAuthorization()
        );
        MultiValueMap<String, String> secondQueryParams = authorizationQueryParams(
                requestAuthorization()
        );

        assertThat(firstQueryParams.getFirst("state"))
                .isNotEqualTo(secondQueryParams.getFirst("state"));
        assertThat(firstQueryParams.getFirst("code_challenge"))
                .isNotEqualTo(secondQueryParams.getFirst("code_challenge"));
    }

    @Test
    @DisplayName("GitHub 화면에서 돌아와 다시 로그인하면 기존 시도로 이동하고 Callback은 성공한다")
    void resumesWaitingAttemptWithoutReplacingIt() {
        Response authorization = requestAuthorization();
        String sessionId = authorization.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorization).getFirst("state");
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("waiting-code"), anyString()))
                .willReturn(new OAuthIdentity(OAuthProvider.GITHUB, UUID.randomUUID().toString(), null));

        Response duplicate = requestAuthorization(sessionId);

        assertThat(duplicate.statusCode()).isEqualTo(302);
        assertThat(duplicate.header("Location")).isEqualTo(authorization.header("Location"));
        assertThat(duplicate.cookie("JSESSIONID")).isNull();
        assertThat(requestCallback(sessionId, state, "waiting-code").statusCode()).isEqualTo(302);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("실제 HTTP Callback 처리 중 중복 요청을 거절해도 신규와 기존 로그인은 회전해 완료된다")
    void rejectsDuplicatesWhileCallbackIsProcessing(boolean existingUser) throws Exception {
        String providerAccountId = UUID.randomUUID().toString();
        String expectedStatus = "SIGNUP_REQUIRED";
        Long expectedUserId = null;
        if (existingUser) {
            OAuthSignupAcceptanceResult signup = completeOAuthSignup();
            providerAccountId = signup.providerAccountId();
            expectedUserId = signup.userId();
            expectedStatus = "AUTHENTICATED";
        }
        OAuthIdentity identity = new OAuthIdentity(OAuthProvider.GITHUB, providerAccountId, null);
        Response authorization = requestAuthorization();
        String sessionId = authorization.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorization).getFirst("state");
        Response resumed = requestAuthorization(sessionId);
        assertThat(resumed.statusCode()).isEqualTo(302);
        assertThat(resumed.header("Location")).isEqualTo(authorization.header("Location"));
        CountDownLatch providerEntered = new CountDownLatch(1);
        CountDownLatch providerRelease = new CountDownLatch(1);
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("processing-code"), anyString()))
                .willAnswer(invocation -> {
                    providerEntered.countDown();
                    assertThat(providerRelease.await(15, TimeUnit.SECONDS)).isTrue();
                    return identity;
                });

        Response completed;
        try (var executor = Executors.newSingleThreadExecutor()) {
            var callback = executor.submit(() -> requestCallback(sessionId, state, "processing-code"));
            try {
                assertThat(providerEntered.await(10, TimeUnit.SECONDS)).isTrue();
                Response duplicateStart = requestAuthorization(sessionId);
                Response duplicateCallback = requestCallback(sessionId, state, "processing-code");
                Response otherCodeCallback = requestCallback(sessionId, state, "other-code");

                assertThat(duplicateStart.statusCode()).isEqualTo(409);
                assertThat(duplicateCallback.statusCode()).isEqualTo(409);
                assertThat(otherCodeCallback.statusCode()).isEqualTo(409);
                assertThat(duplicateStart.jsonPath().getString("code"))
                        .isEqualTo("OAUTH_LOGIN_IN_PROGRESS");
                assertThat(duplicateCallback.jsonPath().getString("code"))
                        .isEqualTo("OAUTH_LOGIN_IN_PROGRESS");
                assertThat(duplicateStart.cookie("JSESSIONID")).isNull();
                assertThat(duplicateCallback.cookie("JSESSIONID")).isNull();
                assertThat(otherCodeCallback.cookie("JSESSIONID")).isNull();
            } finally {
                providerRelease.countDown();
            }
            completed = callback.get(10, TimeUnit.SECONDS);
        }

        assertThat(completed.statusCode()).isEqualTo(302);
        String rotatedSessionId = completed.cookie("JSESSIONID");
        assertThat(rotatedSessionId).isNotBlank().isNotEqualTo(sessionId);
        verify(gitHubOAuthIdentityPort).fetchIdentity(eq("processing-code"), anyString());
        verify(gitHubOAuthIdentityPort, never()).fetchIdentity(eq("other-code"), anyString());
        Response session = RestAssured.given().port(port)
                .cookie("JSESSIONID", rotatedSessionId).get(AUTH_SESSION_PATH);
        assertThat(session.jsonPath().getString("data.status")).isEqualTo(expectedStatus);
        if (existingUser) {
            assertThat(session.jsonPath().getLong("data.userId")).isEqualTo(expectedUserId);
        }
        Response oldCookieCallback = requestCallback(sessionId, state, "other-code");
        assertThat(oldCookieCallback.statusCode()).isEqualTo(400);
        assertThat(oldCookieCallback.jsonPath().getString("code"))
                .isEqualTo("OAUTH_LOGIN_SESSION_NOT_FOUND");
        assertThat(oldCookieCallback.cookie("JSESSIONID")).isNull();
        Response replay = requestCallback(rotatedSessionId, state, "other-code");
        assertThat(replay.statusCode()).isEqualTo(400);
        assertThat(replay.jsonPath().getString("code"))
                .isEqualTo("OAUTH_LOGIN_ATTEMPT_NOT_FOUND");
        assertThat(replay.cookie("JSESSIONID")).isNull();
        Response preserved = RestAssured.given().port(port)
                .cookie("JSESSIONID", rotatedSessionId).get(AUTH_SESSION_PATH);
        assertThat(preserved.jsonPath().getString("data.status")).isEqualTo(expectedStatus);
        assertThat(preserved.jsonPath().getString("data.userId"))
                .isEqualTo(session.jsonPath().getString("data.userId"));
        verify(gitHubOAuthIdentityPort, never()).fetchIdentity(eq("other-code"), anyString());
        assertThat(requestAuthorization(rotatedSessionId).statusCode()).isEqualTo(302);
    }

    @Test
    @DisplayName("같은 쿠키의 동시 로그인 시작은 동일한 인가 URI를 반환한다")
    void resumesSameAttemptForConcurrentHttpStarts() throws Exception {
        String sessionId = RestAssured.given().port(port).get(AUTH_SESSION_PATH).cookie("JSESSIONID");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Response> request = () -> {
            ready.countDown();
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return requestAuthorization(sessionId);
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(request);
            var second = executor.submit(request);
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            Response firstResponse = first.get(10, TimeUnit.SECONDS);
            Response secondResponse = second.get(10, TimeUnit.SECONDS);

            assertThat(firstResponse.statusCode()).isEqualTo(302);
            assertThat(secondResponse.statusCode()).isEqualTo(302);
            assertThat(secondResponse.header("Location")).isEqualTo(firstResponse.header("Location"));
            assertThat(firstResponse.cookie("JSESSIONID")).isNull();
            assertThat(secondResponse.cookie("JSESSIONID")).isNull();
        }
    }

    @Test
    @DisplayName("Provider 처리 실패 후 같은 세션에서 OAuth를 다시 시작할 수 있다")
    void allowsRestartAfterProviderFailure() {
        Response authorization = requestAuthorization();
        String sessionId = authorization.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorization).getFirst("state");
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("failed-code"), anyString()))
                .willThrow(new IllegalStateException("GitHub 응답 실패"));

        Response failed = requestCallback(sessionId, state, "failed-code");

        assertThat(failed.statusCode()).isEqualTo(500);
        assertThat(failed.cookie("JSESSIONID")).isNull();
        Response restarted = requestAuthorization(sessionId);
        assertThat(authorizationQueryParams(restarted).getFirst("state")).isNotEqualTo(state);
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("retry-code"), anyString()))
                .willReturn(new OAuthIdentity(OAuthProvider.GITHUB, UUID.randomUUID().toString(), null));
        assertThat(requestCallback(
                sessionId,
                authorizationQueryParams(restarted).getFirst("state"),
                "retry-code"
        ).statusCode()).isEqualTo(302);
    }

    @Test
    @DisplayName("잘못된 state는 전용 오류로 거부하되 첫 로그인 시도를 없애지 않는다")
    void rejectsWrongStateWithoutConsumingOriginalAttempt() {
        Response authorization = requestAuthorization();
        String sessionId = authorization.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorization).getFirst("state");

        Response invalid = requestCallback(sessionId, "wrong-state", "invalid-code");

        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(invalid.jsonPath().getString("code")).isEqualTo("OAUTH_STATE_MISMATCH");
        verify(gitHubOAuthIdentityPort, never()).fetchIdentity(eq("invalid-code"), anyString());
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("valid-code"), anyString()))
                .willReturn(new OAuthIdentity(OAuthProvider.GITHUB, UUID.randomUUID().toString(), null));
        assertThat(requestCallback(sessionId, state, "valid-code").statusCode()).isEqualTo(302);
    }

    @Test
    @DisplayName("GitHub Callback의 state와 code를 검증해 OAuth 신원을 확인한다")
    void handlesGitHubCallback() {
        Response authorizationResponse = requestAuthorization();
        String sessionId = authorizationResponse.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorizationResponse).getFirst("state");
        String providerAccountId = Long.toUnsignedString(
                UUID.randomUUID().getMostSignificantBits()
        );
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("authorization-code"), anyString()))
                .willReturn(new OAuthIdentity(
                        OAuthProvider.GITHUB,
                        providerAccountId,
                        "https://avatars.githubusercontent.com/u/12345678"
                ));

        Response callbackResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");

        assertThat(callbackResponse.statusCode()).isEqualTo(302);
        assertThat(callbackResponse.header("Location"))
                .isEqualTo("http://localhost:3000/oauth/callback");
        verify(gitHubOAuthIdentityPort).fetchIdentity(eq("authorization-code"), anyString());
        String signupPendingSessionId = callbackResponse.cookie("JSESSIONID");
        assertThat(signupPendingSessionId).isNotBlank().isNotEqualTo(sessionId);

        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupPendingSessionId)
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(sessionResponse.statusCode()).isEqualTo(200);
        assertThat(sessionResponse.jsonPath().getString("status")).isEqualTo("success");
        assertThat(sessionResponse.jsonPath().getString("data.status"))
                .isEqualTo("SIGNUP_REQUIRED");
        assertThat(sessionResponse.jsonPath().getString("data.csrfToken")).isNotBlank();
    }

    @Test
    @DisplayName("OAuth 로그인 세션 없이 Callback을 요청하면 공통 오류 형식으로 거부한다")
    void rejectsGitHubCallbackWithoutOAuthLoginSession() {
        Response response = RestAssured.given()
                .port(port)
                .queryParam("code", "authorization-code")
                .queryParam("state", "state")
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("status")).isEqualTo("error");
        assertThat(response.jsonPath().getString("code"))
                .isEqualTo("OAUTH_LOGIN_SESSION_NOT_FOUND");
    }

    @Test
    @DisplayName("GitHub OAuth 동의를 거절하면 로그인 시도를 종료하고 비로그인 상태로 돌아간다")
    void handlesDeniedGitHubAuthorization() {
        Response authorizationResponse = requestAuthorization();
        String sessionId = authorizationResponse.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorizationResponse).getFirst("state");

        Response callbackResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("error", "access_denied")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");

        assertThat(callbackResponse.statusCode()).isEqualTo(302);
        assertThat(callbackResponse.header("Location"))
                .isEqualTo("http://localhost:3000/oauth/callback");

        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(sessionResponse.statusCode()).isEqualTo(200);
        assertThat(sessionResponse.jsonPath().getString("data.status"))
                .isEqualTo("UNAUTHENTICATED");
        assertThat(requestAuthorization(sessionId).statusCode()).isEqualTo(302);
    }

    @Test
    @DisplayName("인증되지 않은 세션 상태와 CSRF 토큰을 조회한다")
    void getsUnauthenticatedSessionWithCsrfToken() {
        Response response = RestAssured.given()
                .port(port)
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.cookie("JSESSIONID")).isNotBlank();
        assertThat(response.header("Set-Cookie"))
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        assertThat(response.header("Cache-Control")).isEqualTo("no-store");
        assertThat(response.jsonPath().getString("status")).isEqualTo("success");
        assertThat(response.jsonPath().getString("data.status"))
                .isEqualTo("UNAUTHENTICATED");
        assertThat(response.jsonPath().getString("data.csrfToken")).isNotBlank();
    }

    @Test
    @DisplayName("가입 대기 세션의 상태 변경 요청은 도메인과 무관하게 CSRF 토큰이 필요하다")
    void requiresCsrfTokenForStateChangingRequest() {
        Response authorizationResponse = requestAuthorization();
        String sessionId = authorizationResponse.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorizationResponse).getFirst("state");
        String providerAccountId = Long.toUnsignedString(
                UUID.randomUUID().getMostSignificantBits()
        );
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("authorization-code"), anyString()))
                .willReturn(new OAuthIdentity(
                        OAuthProvider.GITHUB,
                        providerAccountId,
                        "https://avatars.githubusercontent.com/u/12345678"
                ));
        Response callbackResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");
        String signupPendingSessionId = callbackResponse.cookie("JSESSIONID");

        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupPendingSessionId)
                .when()
                .get(AUTH_SESSION_PATH);
        String csrfToken = sessionResponse.jsonPath().getString("data.csrfToken");

        Response rejectedResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupPendingSessionId)
                .when()
                .post(CSRF_TEST_PATH);
        Response acceptedResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupPendingSessionId)
                .header("X-CSRF-Token", csrfToken)
                .when()
                .post(CSRF_TEST_PATH);

        assertThat(rejectedResponse.statusCode()).isEqualTo(403);
        assertThat(rejectedResponse.jsonPath().getString("status")).isEqualTo("error");
        assertThat(rejectedResponse.jsonPath().getString("code"))
                .isEqualTo("CSRF_TOKEN_INVALID");
        assertThat(acceptedResponse.statusCode()).isEqualTo(204);
    }

    @Test
    @DisplayName("신규 OAuth 사용자가 프로필을 입력하면 가입과 로그인을 완료한다")
    void signsUpNewOAuthUser() {
        OAuthSignupAcceptanceResult result = completeOAuthSignup();

        User user = userRepository.findById(result.userId()).orElseThrow();
        UserProfile profile = userProfileRepository.findByUserId(result.userId()).orElseThrow();
        OAuthAccount account = oauthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GITHUB,
                result.providerAccountId()
        ).orElseThrow();
        assertThat(user.getHandle()).isEqualTo(new Handle(result.handle()));
        assertThat(user.getLastLoginAt()).isNotNull();
        assertThat(profile.getDisplayName()).isEqualTo(new ProfileDisplayName("상준"));
        assertThat(profile.getAvatarImageId()).isNull();
        assertThat(profile.getGithubProfileUrl()).isNull();
        assertThat(account.getUserId()).isEqualTo(result.userId());
        assertThat(account.getProviderAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/12345678");
        assertThat(account.getLastLoginAt()).isEqualTo(user.getLastLoginAt());

        Response authenticatedSessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", result.sessionId())
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(authenticatedSessionResponse.jsonPath().getString("data.status"))
                .isEqualTo("AUTHENTICATED");
        assertThat(authenticatedSessionResponse.jsonPath().getLong("data.userId"))
                .isEqualTo(result.userId());
    }

    @Test
    @DisplayName("가입 대기 OAuth 신원 없이 가입을 요청하면 공통 오류 형식으로 거부한다")
    void rejectsSignupWithoutPendingOAuthIdentity() {
        Response response = RestAssured.given()
                .port(port)
                .contentType("application/json")
                .body(Map.of(
                        "handle", "@sangjun",
                        "displayName", "상준"
                ))
                .when()
                .post(OAUTH_SIGNUP_PATH);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("status")).isEqualTo("error");
        assertThat(response.jsonPath().getString("code"))
                .isEqualTo("OAUTH_SIGNUP_SESSION_NOT_FOUND");
    }

    @Test
    @DisplayName("기존 GitHub OAuth 계정은 가입 절차 없이 연결된 사용자로 다시 로그인한다")
    void logsInExistingGitHubOAuthUser() {
        OAuthSignupAcceptanceResult signupResult = completeOAuthSignup();
        Response authorizationResponse = requestAuthorization();
        String anonymousSessionId = authorizationResponse.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorizationResponse).getFirst("state");
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("authorization-code"), anyString()))
                .willReturn(new OAuthIdentity(
                        OAuthProvider.GITHUB,
                        signupResult.providerAccountId(),
                        "https://avatars.githubusercontent.com/u/87654321",
                        "https://github.com/sangjun"
                ));

        Response callbackResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", anonymousSessionId)
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");

        String authenticatedSessionId = callbackResponse.cookie("JSESSIONID");
        assertThat(callbackResponse.statusCode()).isEqualTo(302);
        assertThat(authenticatedSessionId)
                .isNotBlank()
                .isNotEqualTo(anonymousSessionId);

        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", authenticatedSessionId)
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(sessionResponse.statusCode()).isEqualTo(200);
        assertThat(sessionResponse.jsonPath().getString("data.status"))
                .isEqualTo("AUTHENTICATED");
        assertThat(sessionResponse.jsonPath().getLong("data.userId"))
                .isEqualTo(signupResult.userId());
        assertThat(sessionResponse.jsonPath().getString("data.role"))
                .isEqualTo("USER");
        assertThat(oauthAccountRepository.findByProviderAndProviderAccountId(
                OAuthProvider.GITHUB,
                signupResult.providerAccountId()
        ).orElseThrow().getProviderAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/87654321");
    }

    @Test
    @DisplayName("인증 사용자는 CSRF 토큰으로 로그아웃하고 기존 세션을 폐기한다")
    void logsOutAuthenticatedUser() {
        OAuthSignupAcceptanceResult signupResult = completeOAuthSignup();

        Response rejectedResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupResult.sessionId())
                .when()
                .post(LOGOUT_PATH);
        Response logoutResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupResult.sessionId())
                .header("X-CSRF-Token", signupResult.csrfToken())
                .when()
                .post(LOGOUT_PATH);
        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupResult.sessionId())
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(rejectedResponse.statusCode()).isEqualTo(403);
        assertThat(rejectedResponse.jsonPath().getString("code"))
                .isEqualTo("CSRF_TOKEN_INVALID");
        assertThat(logoutResponse.statusCode()).isEqualTo(204);
        assertThat(sessionResponse.jsonPath().getString("data.status"))
                .isEqualTo("UNAUTHENTICATED");
    }

    @Test
    @DisplayName("로그인이 필요한 API에 인증 사용자 정보를 주입한다")
    void injectsAuthenticatedUser() {
        Response unauthenticatedResponse = RestAssured.given()
                .port(port)
                .when()
                .get(AUTHENTICATED_USER_TEST_PATH);
        OAuthSignupAcceptanceResult signupResult = completeOAuthSignup();

        Response authenticatedResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupResult.sessionId())
                .when()
                .get(AUTHENTICATED_USER_TEST_PATH);

        assertThat(unauthenticatedResponse.statusCode()).isEqualTo(401);
        assertThat(unauthenticatedResponse.jsonPath().getString("status")).isEqualTo("error");
        assertThat(unauthenticatedResponse.jsonPath().getString("code"))
                .isEqualTo("UNAUTHORIZED");
        assertThat(authenticatedResponse.statusCode()).isEqualTo(200);
        assertThat(authenticatedResponse.jsonPath().getLong("userId"))
                .isEqualTo(signupResult.userId());
        assertThat(authenticatedResponse.jsonPath().getString("role"))
                .isEqualTo("USER");
    }

    private OAuthSignupAcceptanceResult completeOAuthSignup() {
        Response authorizationResponse = requestAuthorization();
        String sessionId = authorizationResponse.cookie("JSESSIONID");
        String state = authorizationQueryParams(authorizationResponse).getFirst("state");
        String providerAccountId = Long.toUnsignedString(
                UUID.randomUUID().getMostSignificantBits()
        );
        given(gitHubOAuthIdentityPort.fetchIdentity(eq("authorization-code"), anyString()))
                .willReturn(new OAuthIdentity(
                        OAuthProvider.GITHUB,
                        providerAccountId,
                        "https://avatars.githubusercontent.com/u/12345678",
                        "https://github.com/sangjun"
                ));
        Response callbackResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");
        String signupPendingSessionId = callbackResponse.cookie("JSESSIONID");
        Response pendingSessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupPendingSessionId)
                .when()
                .get(AUTH_SESSION_PATH);
        String csrfToken = pendingSessionResponse.jsonPath().getString("data.csrfToken");
        String handle = "@sangjun-" + UUID.randomUUID().toString().substring(0, 8);

        Response signupResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", signupPendingSessionId)
                .header("X-CSRF-Token", csrfToken)
                .contentType("application/json")
                .body(Map.of(
                        "handle", handle,
                        "displayName", "상준"
                ))
                .when()
                .post(OAUTH_SIGNUP_PATH);

        assertThat(signupResponse.statusCode()).isEqualTo(201);
        assertThat(signupResponse.jsonPath().getString("status")).isEqualTo("success");
        assertThat(signupResponse.jsonPath().getLong("data.userId")).isPositive();
        long userId = signupResponse.jsonPath().getLong("data.userId");
        String authenticatedSessionId = signupResponse.cookie("JSESSIONID");
        assertThat(authenticatedSessionId)
                .isNotBlank()
                .isNotEqualTo(signupPendingSessionId);
        return new OAuthSignupAcceptanceResult(
                authenticatedSessionId,
                csrfToken,
                userId,
                providerAccountId,
                handle
        );
    }

    private Response requestAuthorization() {
        return RestAssured.given()
                .port(port)
                .redirects()
                .follow(false)
                .when()
                .get(GITHUB_AUTHORIZATION_PATH);
    }

    private Response requestAuthorization(String sessionId) {
        return RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .redirects().follow(false)
                .get(GITHUB_AUTHORIZATION_PATH);
    }

    private Response requestCallback(String sessionId, String state, String code) {
        return RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("state", state)
                .queryParam("code", code)
                .redirects().follow(false)
                .get("/login/oauth2/code/github");
    }

    private MultiValueMap<String, String> authorizationQueryParams(Response response) {
        assertThat(response.statusCode()).isEqualTo(302);
        return UriComponentsBuilder.fromUriString(response.header("Location"))
                .build()
                .getQueryParams();
    }

    private record OAuthSignupAcceptanceResult(
            String sessionId,
            String csrfToken,
            long userId,
            String providerAccountId,
            String handle
    ) {
    }

    @RestController
    static class TestAuthHttpApi {

        @PostMapping(CSRF_TEST_PATH)
        ResponseEntity<Void> changeState() {
            return ResponseEntity.noContent().build();
        }

        @GetMapping(AUTHENTICATED_USER_TEST_PATH)
        ResponseEntity<AuthenticatedUser> getAuthenticatedUser(
                @LoginUser AuthenticatedUser authenticatedUser
        ) {
            return ResponseEntity.ok(authenticatedUser);
        }
    }
}
