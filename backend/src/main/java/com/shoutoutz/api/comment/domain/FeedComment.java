package com.shoutoutz.api.comment.domain;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
public class FeedComment {

    private final Long id;
    private final Long feedId;
    private final Long authorId;
    private final Long parentId;
    private final String content;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    @Builder
    private FeedComment(
            Long id,
            Long feedId,
            Long authorId,
            Long parentId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.feedId = feedId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static FeedComment create(
            long feedId,
            long authorId,
            Long parentId,
            String content
    ) {
        return FeedComment.builder()
                .feedId(feedId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content == null ? null : content.strip())
                .build();
    }

    /**
     * DB에서 꺼내어 재조립하는 경우
     */
    public static FeedComment reconstitute(
            Long id,
            Long feedId,
            Long authorId,
            Long parentId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return FeedComment.builder()
                .id(id)
                .feedId(feedId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
