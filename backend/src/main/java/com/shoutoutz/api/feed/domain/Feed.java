package com.shoutoutz.api.feed.domain;

import java.time.Instant;
import lombok.Getter;

@Getter
public final class Feed {

    private final Long id;
    private final Long authorId;
    private final FeedType type;
    private final String title;
    private final String content;
    private final boolean anonymous;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    private Feed(
            Long id,
            Long authorId,
            FeedType type,
            String title,
            String content,
            boolean anonymous,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        FeedValidator.validate(
                id,
                authorId,
                type,
                title,
                content,
                createdAt,
                updatedAt,
                deletedAt
        );
        this.id = id;
        this.authorId = authorId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.anonymous = anonymous;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Feed create(long authorId, String title, String content, Instant now) {
        return create(authorId, FeedType.POST, title, content, false, now);
    }

    public static Feed create(
            long authorId,
            String title,
            String content,
            boolean anonymous,
            Instant now
    ) {
        return create(authorId, FeedType.POST, title, content, anonymous, now);
    }

    public static Feed create(
            long authorId,
            FeedType type,
            String title,
            String content,
            boolean anonymous,
            Instant now
    ) {
        return new Feed(null, authorId, type, title, content, anonymous, now, now, null);
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
        return reconstitute(
                id,
                authorId,
                FeedType.POST,
                title,
                content,
                false,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static Feed reconstitute(
            long id,
            long authorId,
            FeedType type,
            String title,
            String content,
            boolean anonymous,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return new Feed(
                id,
                authorId,
                type,
                title,
                content,
                anonymous,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public Feed update(String title, String content, Instant updatedAt) {
        return update(title, content, anonymous, updatedAt);
    }

    public Feed update(String title, String content, boolean anonymous, Instant updatedAt) {
        return new Feed(id, authorId, type, title, content, anonymous, createdAt, updatedAt, deletedAt);
    }

    public Feed delete(Instant deletedAt) {
        return new Feed(id, authorId, type, title, content, anonymous, createdAt, deletedAt, deletedAt);
    }

    public boolean isWrittenBy(long userId) {
        return authorId == userId;
    }
}
