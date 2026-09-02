package com.shoutoutz.api.auth.application;

import com.shoutoutz.api.auth.application.port.GitHubOAuthAuthorizationPort;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final GitHubOAuthAuthorizationPort githubOAuthAuthorizationPort;

    public OAuthLoginStartResult startGitHubLogin() {
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
        URI authorizationUri = githubOAuthAuthorizationPort.generateAuthorizationUri(
                attempt.state(),
                attempt.codeChallenge()
        );

        return new OAuthLoginStartResult(authorizationUri, attempt);
    }
}
