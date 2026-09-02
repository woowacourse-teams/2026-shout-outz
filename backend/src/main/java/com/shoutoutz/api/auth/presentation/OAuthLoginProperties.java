package com.shoutoutz.api.auth.presentation;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.oauth")
public record OAuthLoginProperties(
        URI completionUri
) {
}
