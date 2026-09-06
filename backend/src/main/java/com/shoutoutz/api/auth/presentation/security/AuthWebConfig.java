package com.shoutoutz.api.auth.presentation.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AuthCorsProperties.class)
@RequiredArgsConstructor
class AuthWebConfig implements WebMvcConfigurer {

    private static final String API_PATH = "/api/**";

    private final LoginUserArgumentResolver loginUserArgumentResolver;
    private final AuthCorsProperties authCorsProperties;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(API_PATH)
                .allowedOrigins(authCorsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "X-CSRF-Token")
                .allowCredentials(true);
    }
}
