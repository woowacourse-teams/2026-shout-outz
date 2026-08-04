package com.dropit.backend.common.auth;

import java.util.Optional;
import java.util.UUID;

import com.dropit.backend.common.api.ApiException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public UUID requireId(Jwt jwt) {
        return optionalId(jwt).orElseThrow(ApiException::unauthorized);
    }

    public Optional<UUID> optionalId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(jwt.getSubject()));
        } catch (IllegalArgumentException exception) {
            throw ApiException.unauthorized();
        }
    }
}
