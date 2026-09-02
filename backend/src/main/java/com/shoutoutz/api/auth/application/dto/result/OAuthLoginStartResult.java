package com.shoutoutz.api.auth.application.dto.result;

import com.shoutoutz.api.auth.application.OAuthLoginAttempt;
import java.net.URI;

public record OAuthLoginStartResult(
        URI authorizationUri,
        OAuthLoginAttempt attempt
) {
}
