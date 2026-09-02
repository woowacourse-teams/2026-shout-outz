package com.shoutoutz.api.auth.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.shoutoutz.api.auth.application.port.GitHubOAuthAuthorizationPort;
import com.shoutoutz.api.auth.application.port.GitHubOAuthIdentityPort;
import com.shoutoutz.api.auth.domain.OAuthIdentity;
import com.shoutoutz.api.auth.domain.OAuthProvider;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class OAuthLoginServiceTest {

    private final GitHubOAuthAuthorizationPort authorizationPort =
            mock(GitHubOAuthAuthorizationPort.class);
    private final GitHubOAuthIdentityPort identityPort = mock(GitHubOAuthIdentityPort.class);
    private final OAuthAccountLoginService oauthAccountLoginService =
            mock(OAuthAccountLoginService.class);

    private OAuthLoginService oauthLoginService;

    @BeforeEach
    void setUp() {
        oauthLoginService = new OAuthLoginService(
                authorizationPort,
                identityPort,
                oauthAccountLoginService
        );
    }

    @Test
    @DisplayName("GitHub 인증 결과를 OAuth 계정 로그인 처리에 전달한다")
    void delegatesGitHubIdentityToOAuthAccountLogin() {
        OAuthLoginAttempt attempt = loginAttempt();
        OAuthIdentity identity = githubIdentity();
        OAuthLoginCallbackResult expected = OAuthLoginCallbackResult.signupRequired(identity);
        given(identityPort.fetchIdentity("authorization-code", "code-verifier"))
                .willReturn(identity);
        given(oauthAccountLoginService.completeLogin(
                ArgumentMatchers.eq(identity),
                ArgumentMatchers.any(Instant.class)
        )).willReturn(expected);

        OAuthLoginCallbackResult result = oauthLoginService.completeGitHubLogin(
                "authorization-code",
                "state",
                attempt
        );

        verify(oauthAccountLoginService).completeLogin(
                ArgumentMatchers.eq(identity),
                ArgumentMatchers.any(Instant.class)
        );
    }

    private OAuthLoginAttempt loginAttempt() {
        return new OAuthLoginAttempt(
                "state",
                "code-verifier",
                Instant.now()
        );
    }

    private OAuthIdentity githubIdentity() {
        return new OAuthIdentity(
                OAuthProvider.GITHUB,
                "12345678",
                "https://avatars.githubusercontent.com/u/12345678"
        );
    }
}
