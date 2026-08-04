package com.dropit.backend.auth;

import java.util.UUID;

public record AuthSessionResponse(UUID id, String name, String email, String avatarUrl) {
}
