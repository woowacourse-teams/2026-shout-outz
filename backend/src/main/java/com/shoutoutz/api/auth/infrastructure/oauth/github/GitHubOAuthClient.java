package com.shoutoutz.api.auth.infrastructure.oauth.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GitHubOAuthClient implements GitHubOAuthIdentityPort {

    private static final String TOKEN_URI = "https://github.com/login/oauth/access_token";
    private static final String USER_URI = "https://api.github.com/user";

    private final RestClient restClient;
    private final GitHubOAuthProperties properties;

    public GitHubOAuthClient(
            RestClient.Builder restClientBuilder,
            GitHubOAuthProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public OAuthIdentity fetchIdentity(String authorizationCode, String codeVerifier) {
        String accessToken = exchangeAccessToken(authorizationCode, codeVerifier);
        GitHubUserResponse user = fetchUser(accessToken);

        return new OAuthIdentity(
                OAuthProvider.GITHUB,
                String.valueOf(Objects.requireNonNull(user.id(), "GitHub 사용자 ID가 없습니다.")),
                user.avatarUrl()
        );
    }

    private String exchangeAccessToken(String authorizationCode, String codeVerifier) {
        MultiValueMap<String, String> request = new LinkedMultiValueMap<>();
        request.add("client_id", properties.clientId());
        request.add("client_secret", properties.clientSecret());
        request.add("code", authorizationCode);
        request.add("redirect_uri", properties.redirectUri().toString());
        request.add("code_verifier", codeVerifier);

        GitHubAccessTokenResponse response = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GitHubAccessTokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("GitHub Access Token이 없습니다.");
        }
        return response.accessToken();
    }

    private GitHubUserResponse fetchUser(String accessToken) {
        GitHubUserResponse response = restClient.get()
                .uri(USER_URI)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubUserResponse.class);

        return Objects.requireNonNull(response, "GitHub 사용자 정보가 없습니다.");
    }

    private record GitHubAccessTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }

    private record GitHubUserResponse(
            Long id,
            @JsonProperty("avatar_url") String avatarUrl
    ) {
    }
}
