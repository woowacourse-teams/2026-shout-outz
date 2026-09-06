package com.shoutoutz.api.auth;

import com.shoutoutz.api.auth.infrastructure.oauth.github.GitHubOAuthProperties;
import com.shoutoutz.api.auth.presentation.OAuthLoginProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        GitHubOAuthProperties.class,
        OAuthLoginProperties.class
})
class AuthPropertiesConfig {
}
