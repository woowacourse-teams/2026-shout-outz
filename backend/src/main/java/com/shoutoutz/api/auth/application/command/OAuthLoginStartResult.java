package com.shoutoutz.api.auth.application.command;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import java.net.URI;

public record OAuthLoginStartResult(
        URI authorizationUri,
        OAuthLoginAttempt attempt
) {
}
