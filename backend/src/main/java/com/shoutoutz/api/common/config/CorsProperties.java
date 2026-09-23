package com.shoutoutz.api.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
record CorsProperties(
        List<String> allowedOriginPatterns
) {
}
