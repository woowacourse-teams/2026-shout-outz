package com.dropit.backend.app;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dropit.backend.maker.MakerResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AppItem")
public record AppResponse(
    String id,
    String name,
    String tagline,
    String description,
    List<AppCategory> categories,
    ThumbnailVariant thumbnailVariant,
    String thumbnailUrl,
    String appUrl,
    String githubUrl,
    MakerResponse maker,
    List<String> techTags,
    int plays,
    int likes,
    Instant createdAt,
    UUID ownerId,
    Instant deletedAt,
    String source
) {

    public static AppResponse from(AppEntity app) {
        return new AppResponse(
            app.getId(),
            app.getName(),
            app.getTagline(),
            app.getDescription(),
            app.getCategories().stream().map(AppCategory::fromValue).toList(),
            ThumbnailVariant.fromValue(app.getThumbnailVariant()),
            app.getThumbnailUrl(),
            app.getAppUrl(),
            app.getGithubUrl(),
            makerFromSnapshot(app.getMaker()),
            app.getTechTags() == null ? List.of() : List.copyOf(app.getTechTags()),
            app.getPlays(),
            app.getLikes(),
            app.getCreatedAt(),
            app.getOwnerId(),
            app.getDeletedAt(),
            app.getSource()
        );
    }

    private static MakerResponse makerFromSnapshot(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new MakerResponse(
            uuid(snapshot.get("id")),
            stringValue(snapshot.get("name")),
            stringValue(snapshot.get("initials")),
            stringValue(snapshot.get("avatarUrl")),
            stringValue(snapshot.get("role")),
            stringValue(snapshot.get("bio")),
            stringValue(snapshot.get("tone"))
        );
    }

    private static UUID uuid(Object value) {
        return value == null ? null : UUID.fromString(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
