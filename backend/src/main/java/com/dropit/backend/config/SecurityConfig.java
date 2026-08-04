package com.dropit.backend.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/v1/openapi.json",
                    "/v1/swagger-ui.html",
                    "/v1/swagger-ui/**",
                    "/actuator/health",
                    "/error"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
            }));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(SupabaseProperties properties) {
        SupabaseProperties.Jwt jwt = properties.jwt();
        String issuer = valueOrEmpty(jwt == null ? null : jwt.issuer());
        String secret = valueOrEmpty(jwt == null ? null : jwt.secret());
        String jwkSetUri = valueOrEmpty(jwt == null ? null : jwt.jwkSetUri());

        NimbusJwtDecoder decoder;
        if (!secret.isBlank()) {
            decoder = NimbusJwtDecoder.withSecretKey(
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
                )
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        } else if (!jwkSetUri.isBlank()) {
            decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        } else {
            throw new IllegalStateException(
                "SUPABASE_JWT_SECRET 또는 SUPABASE_JWT_JWK_SET_URI 중 하나는 설정해야 합니다."
            );
        }

        decoder.setJwtValidator(issuer.isBlank()
            ? JwtValidators.createDefault()
            : JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(DropitCorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.getAllowedOriginList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
