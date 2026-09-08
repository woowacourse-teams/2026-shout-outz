package com.shoutoutz.api.auth.application;

import com.shoutoutz.api.auth.application.port.GitHubOAuthAuthorizationPort;
import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.application.dto.result.OAuthLoginCallbackResult;
import com.shoutoutz.api.auth.application.dto.result.OAuthLoginStartResult;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final GitHubOAuthAuthorizationPort githubOAuthAuthorizationPort;
    private final GitHubOAuthIdentityPort githubOAuthIdentityPort;
    private final OAuthAccountLoginService oauthAccountLoginService;

    public OAuthLoginStartResult startGitHubLogin() {
        OAuthLoginAttempt attempt = OAuthLoginAttempt.create();
        URI authorizationUri = githubOAuthAuthorizationPort.generateAuthorizationUri(
                attempt.state(),
                attempt.codeChallenge()
        );

        return new OAuthLoginStartResult(authorizationUri, attempt);
    }

    public OAuthLoginCallbackResult completeGitHubLogin(
            String authorizationCode,
            String state,
            OAuthLoginAttempt attempt
    ) {
        Instant authenticatedAt = Instant.now();
        validateGitHubCallback(state, attempt, authenticatedAt);
        OAuthIdentity identity = githubOAuthIdentityPort.fetchIdentity(
                authorizationCode,
                attempt.codeVerifier()
        );

        return oauthAccountLoginService.completeLogin(identity, authenticatedAt);
    }

    public void validateGitHubCallback(String state, OAuthLoginAttempt attempt) {
        validateGitHubCallback(state, attempt, Instant.now());
    }

    private void validateGitHubCallback(
            String state,
            OAuthLoginAttempt attempt,
            Instant validatedAt
    ) {
        attempt.validateCallback(state, validatedAt);
    }
}
