package com.dropit.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "supabase")
public record SupabaseProperties(String url, Jwt jwt) {

    public record Jwt(String issuer, String jwkSetUri, String secret) {
    }
}
