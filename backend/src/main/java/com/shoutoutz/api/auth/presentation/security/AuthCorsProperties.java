package com.shoutoutz.api.auth.presentation.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.cors")
record AuthCorsProperties(
        List<String> allowedOrigins
) {
}
