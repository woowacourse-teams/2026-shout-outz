package com.shoutoutz.api.auth.infrastructure.oauth.github;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github.oauth")
public record GitHubOAuthProperties(
        String clientId,
        URI redirectUri
) {
}
