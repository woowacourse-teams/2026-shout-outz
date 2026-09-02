package com.shoutoutz.api.auth.application.port;

import com.shoutoutz.api.auth.domain.OAuthIdentity;

public interface GitHubOAuthIdentityPort {

    OAuthIdentity fetchIdentity(String authorizationCode, String codeVerifier);
}
