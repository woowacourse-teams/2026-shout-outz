package com.shoutoutz.api.auth.application;

import java.net.URI;

public record OAuthLoginStartResult(
        URI authorizationUri,
        OAuthLoginAttempt attempt
) {
}
