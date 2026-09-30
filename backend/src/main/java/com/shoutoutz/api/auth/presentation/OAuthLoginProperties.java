package com.shoutoutz.api.auth.presentation;

import com.shoutoutz.api.auth.exception.AuthErrorCode;
import com.shoutoutz.api.common.exception.custom.BadRequestException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.oauth")
public record OAuthLoginProperties(
        URI completionUri,
        List<URI> allowedCompletionUris
) {
    public OAuthLoginProperties {
        Objects.requireNonNull(completionUri, "OAuth 완료 URI가 없습니다.");

        LinkedHashSet<URI> configuredUris = new LinkedHashSet<>();
        configuredUris.add(completionUri);
        if (allowedCompletionUris != null) {
            configuredUris.addAll(allowedCompletionUris);
        }
        allowedCompletionUris = List.copyOf(configuredUris);
    }

    public URI resolveCompletionUri(URI requestedUri) {
        if (requestedUri == null) {
            return completionUri;
        }
        if (!allowedCompletionUris.contains(requestedUri)) {
            throw new BadRequestException(AuthErrorCode.OAUTH_COMPLETION_URI_NOT_ALLOWED);
        }
        return requestedUri;
    }
}
