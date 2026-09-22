package com.shoutoutz.api.feed.domain;

import java.time.Instant;
import lombok.Getter;

@Getter
public final class Feed {

    private final Long id;
    private final Long authorId;
    private final String title;
    private final String content;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    private Feed(
            Long id,
            Long authorId,
            String title,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        FeedValidator.validate(
                id,
                authorId,
                title,
                content,
                createdAt,
                updatedAt,
                deletedAt
        );
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Feed create(long authorId, String title, String content, Instant now) {
        return new Feed(null, authorId, title, content, now, now, null);
    }

    public static Feed reconstitute(
            long id,
            long authorId,
            String title,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return new Feed(id, authorId, title, content, createdAt, updatedAt, deletedAt);
    }

    public Feed update(String title, String content, Instant updatedAt) {
        return new Feed(id, authorId, title, content, createdAt, updatedAt, deletedAt);
    }

    public Feed delete(Instant deletedAt) {
        return new Feed(id, authorId, title, content, createdAt, deletedAt, deletedAt);
    }

    public boolean isWrittenBy(long userId) {
        return authorId == userId;
    }
}
