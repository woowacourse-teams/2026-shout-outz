package com.shoutoutz.api.auth.presentation.security;

import com.shoutoutz.api.auth.presentation.session.AuthSessionAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AuthFilterConfig {

    @Bean
    SessionAuthenticationFilter sessionAuthenticationFilter(
            AuthSessionAccessor authSessionAccessor
    ) {
        return new SessionAuthenticationFilter(authSessionAccessor);
    }

    @Bean
    CsrfProtectionFilter csrfProtectionFilter(
            CsrfTokenManager csrfTokenManager,
            AuthSessionAccessor authSessionAccessor
    ) {
        return new CsrfProtectionFilter(csrfTokenManager, authSessionAccessor);
    }
}
