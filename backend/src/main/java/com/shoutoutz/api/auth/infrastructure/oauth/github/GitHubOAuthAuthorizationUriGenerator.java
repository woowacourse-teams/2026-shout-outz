package com.shoutoutz.api.auth.infrastructure.oauth.github;

import com.shoutoutz.api.auth.application.port.GitHubOAuthAuthorizationPort;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GitHubOAuthAuthorizationUriGenerator implements GitHubOAuthAuthorizationPort {

    private static final String AUTHORIZATION_URI =
            "https://github.com/login/oauth/authorize";

    private final GitHubOAuthProperties properties;

    @Override
    public URI generateAuthorizationUri(String state, String codeChallenge) {
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URI)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUri();
    }
}
