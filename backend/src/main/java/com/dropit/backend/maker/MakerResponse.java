package com.dropit.backend.maker;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Maker")
public record MakerResponse(
    UUID id,
    String name,
    String initials,
    String avatarUrl,
    String role,
    String bio,
    String tone
) {

    public static MakerResponse from(MakerEntity maker) {
        return new MakerResponse(
            maker.getId(),
            maker.getName(),
            maker.getInitials(),
            maker.getAvatarUrl(),
            maker.getRole(),
            maker.getBio(),
            maker.getTone()
        );
    }
}
