package com.shoutoutz.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.domain.OAuthAccount;
import com.shoutoutz.api.auth.domain.OAuthAccountRepository;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.auth.presentation.security.AuthenticatedUserId;
import com.shoutoutz.api.user.domain.Handle;
import com.shoutoutz.api.user.domain.ProfileDisplayName;
import com.shoutoutz.api.user.domain.User;
import com.shoutoutz.api.user.domain.UserProfile;
import com.shoutoutz.api.user.domain.UserProfileRepository;
import com.shoutoutz.api.user.domain.UserRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(sessionResponse.statusCode()).isEqualTo(200);
        assertThat(sessionResponse.jsonPath().getString("status"))
                .isEqualTo("SIGNUP_REQUIRED");
        assertThat(sessionResponse.jsonPath().getString("csrfToken")).isNotBlank();
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
        assertThat(response.jsonPath().getString("status"))
                .isEqualTo("UNAUTHENTICATED");
        assertThat(response.jsonPath().getString("csrfToken")).isNotBlank();
    }

    @Test
    @DisplayName("허용된 프론트엔드 Origin의 세션 API 요청을 허용한다")
    void allowsConfiguredFrontendOrigin() {
        Response response = RestAssured.given()
                .port(port)
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options(AUTH_SESSION_PATH);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.header("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:5173");
        assertThat(response.header("Access-Control-Allow-Credentials"))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("허용되지 않은 Origin의 세션 API 요청을 거부한다")
    void rejectsUnknownOrigin() {
        Response response = RestAssured.given()
                .port(port)
                .header("Origin", "https://attacker.example")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options(AUTH_SESSION_PATH);

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.header("Access-Control-Allow-Origin")).isNull();
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
        RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");

        Response sessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .when()
                .get(AUTH_SESSION_PATH);
        String csrfToken = sessionResponse.jsonPath().getString("csrfToken");

        Response rejectedResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .when()
                .post(CSRF_TEST_PATH);
        Response acceptedResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .header("X-CSRF-Token", csrfToken)
                .when()
                .post(CSRF_TEST_PATH);

        assertThat(rejectedResponse.statusCode()).isEqualTo(403);
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
        assertThat(profile.getAvatarUrl())
                .isEqualTo("https://avatars.githubusercontent.com/u/12345678");
        assertThat(profile.getGithubProfileUrl()).isEqualTo("https://github.com/sangjun");
        assertThat(account.getUserId()).isEqualTo(result.userId());
        assertThat(account.getLastLoginAt()).isEqualTo(user.getLastLoginAt());

        Response authenticatedSessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", result.sessionId())
                .when()
                .get(AUTH_SESSION_PATH);

        assertThat(authenticatedSessionResponse.jsonPath().getString("status"))
                .isEqualTo("AUTHENTICATED");
        assertThat(authenticatedSessionResponse.jsonPath().getLong("userId"))
                .isEqualTo(result.userId());
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
        assertThat(logoutResponse.statusCode()).isEqualTo(204);
        assertThat(sessionResponse.jsonPath().getString("status"))
                .isEqualTo("UNAUTHENTICATED");
    }

    @Test
    @DisplayName("로그인이 필요한 API에 인증 사용자 ID를 주입한다")
    void injectsAuthenticatedUserId() {
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
        assertThat(authenticatedResponse.statusCode()).isEqualTo(200);
        assertThat(authenticatedResponse.as(Long.class)).isEqualTo(signupResult.userId());
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
        RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .queryParam("code", "authorization-code")
                .queryParam("state", state)
                .redirects()
                .follow(false)
                .when()
                .get("/login/oauth2/code/github");
        Response pendingSessionResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .when()
                .get(AUTH_SESSION_PATH);
        String csrfToken = pendingSessionResponse.jsonPath().getString("csrfToken");
        String handle = "sangjun-" + UUID.randomUUID().toString().substring(0, 8);

        Response signupResponse = RestAssured.given()
                .port(port)
                .cookie("JSESSIONID", sessionId)
                .header("X-CSRF-Token", csrfToken)
                .contentType("application/json")
                .body(Map.of(
                        "handle", handle,
                        "displayName", "상준",
                        "userType", "GENERAL"
                ))
                .when()
                .post(OAUTH_SIGNUP_PATH);

        assertThat(signupResponse.statusCode()).isEqualTo(201);
        assertThat(signupResponse.jsonPath().getLong("userId")).isPositive();
        long userId = signupResponse.jsonPath().getLong("userId");
        String authenticatedSessionId = signupResponse.cookie("JSESSIONID");
        assertThat(authenticatedSessionId).isNotBlank().isNotEqualTo(sessionId);
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
        ResponseEntity<Long> getAuthenticatedUserId(
                @AuthenticatedUserId Long userId
        ) {
            return ResponseEntity.ok(userId);
        }
    }
}
