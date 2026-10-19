package com.shoutoutz.api.comment.domain;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

@Getter
public class MagazineComment {

    private final Long id;
    private final Long magazineId;
    private final Long authorId;
    private final Long parentId;
    private final String content;
    private final boolean anonymous;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    @Builder
    private MagazineComment(
            Long id,
            Long magazineId,
            Long authorId,
            Long parentId,
            String content,
            boolean anonymous,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.magazineId = magazineId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.anonymous = anonymous;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static MagazineComment create(
            long magazineId,
            long authorId,
            Long parentId,
            String content,
            boolean anonymous
    ) {
        return MagazineComment.builder()
                .magazineId(magazineId)
                .authorId(authorId)
                .parentId(parentId)
                .content(normalizeContent(content))
                .anonymous(anonymous)
                .build();
    }

    public static MagazineComment reconstitute(
            Long id,
            Long magazineId,
            Long authorId,
            Long parentId,
            String content,
            boolean anonymous,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return MagazineComment.builder()
                .id(id)
                .magazineId(magazineId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .anonymous(anonymous)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    private static String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        return content.strip();
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isEdited() {
        return !Objects.equals(createdAt, updatedAt);
    }

    public MagazineComment updateContent(String content) {
        return MagazineComment.builder()
                .id(id)
                .magazineId(magazineId)
                .authorId(authorId)
                .parentId(parentId)
                .content(normalizeContent(content))
                .anonymous(anonymous)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public MagazineComment delete(Instant deletedAt) {
        return MagazineComment.builder()
                .id(id)
                .magazineId(magazineId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .anonymous(anonymous)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
