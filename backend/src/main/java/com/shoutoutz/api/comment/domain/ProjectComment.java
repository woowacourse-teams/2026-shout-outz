package com.shoutoutz.api.comment.domain;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ProjectComment {

    private final Long id;
    private final Long projectId;
    private final Long authorId;
    private final Long parentId;
    private final String content;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    @Builder
    private ProjectComment(
            Long id,
            Long projectId,
            Long authorId,
            Long parentId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.projectId = projectId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static ProjectComment create(
            long projectId,
            long authorId,
            Long parentId,
            String content
    ) {
        return ProjectComment.builder()
                .projectId(projectId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content == null ? null : content.strip())
                .build();
    }

    /**
     * DB에서 꺼내어 재조립하는 경우
     */
    public static ProjectComment reconstitute(
            Long id,
            Long projectId,
            Long authorId,
            Long parentId,
            String content,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return ProjectComment.builder()
                .id(id)
                .projectId(projectId)
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

    public boolean isEdited() {
        return !Objects.equals(createdAt, updatedAt);
    }

    public ProjectComment updateContent(String content) {
        return ProjectComment.builder()
                .id(id)
                .projectId(projectId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content == null ? null : content.strip())
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public ProjectComment delete(Instant deletedAt) {
        return ProjectComment.builder()
                .id(id)
                .projectId(projectId)
                .authorId(authorId)
                .parentId(parentId)
                .content(content)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }
}
