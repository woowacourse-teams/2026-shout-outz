package com.shoutoutz.api.post.domain;

import java.time.Instant;
import lombok.Getter;

@Getter
public final class Post {

    private final Long id;
    private final Long authorId;
    private final String content;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    private Post(
            Long id,
            Long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        PostValidator.validate(
                id,
                authorId,
                content,
                createdAt,
                updatedAt,
                deletedAt
        );
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Post create(long authorId, String content, Instant now) {
        return new Post(null, authorId, content, now, now, null);
    }

    public static Post reconstitute(
            long id,
            long authorId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return new Post(id, authorId, content, createdAt, updatedAt, deletedAt);
    }

    public Post updateContent(String content, Instant updatedAt) {
        return new Post(id, authorId, content, createdAt, updatedAt, deletedAt);
    }

    public Post delete(Instant deletedAt) {
        return new Post(id, authorId, content, createdAt, deletedAt, deletedAt);
    }

    public boolean isWrittenBy(long userId) {
        return authorId == userId;
    }
}
