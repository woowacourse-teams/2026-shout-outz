package com.shoutoutz.api.auth.application.port;

import java.net.URI;

public interface GitHubOAuthAuthorizationPort {

    URI generateAuthorizationUri(String state, String codeChallenge);
}
