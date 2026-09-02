package com.shoutoutz.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuthLoginAcceptanceTest {

    private static final String GITHUB_AUTHORIZATION_PATH =
            "/oauth2/authorization/github";

    @LocalServerPort
    private int port;

    @MockitoBean
    private GitHubOAuthIdentityPort gitHubOAuthIdentityPort;

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
}
