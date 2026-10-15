package com.shoutoutz.api.comment.domain;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

@Getter
public class InterviewComment {

    private final Long id;
    private final Long interviewId;
    private final Long authorId;
    private final Long parentId;
    private final String content;
    private final boolean anonymous;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;

    @Builder
    private InterviewComment(
            Long id,
            Long interviewId,
            Long authorId,
            Long parentId,
            String content,
            boolean anonymous,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.interviewId = interviewId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.content = content;
        this.anonymous = anonymous;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static InterviewComment create(
            long interviewId,
            long authorId,
            Long parentId,
            String content,
            boolean anonymous
    ) {
        return InterviewComment.builder()
                .interviewId(interviewId)
                .authorId(authorId)
                .parentId(parentId)
                .content(normalizeContent(content))
                .anonymous(anonymous)
                .build();
    }

    public static InterviewComment reconstitute(
            Long id,
            Long interviewId,
            Long authorId,
            Long parentId,
            String content,
            boolean anonymous,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        return InterviewComment.builder()
                .id(id)
                .interviewId(interviewId)
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

    public InterviewComment updateContent(String content) {
        return InterviewComment.builder()
                .id(id)
                .interviewId(interviewId)
                .authorId(authorId)
                .parentId(parentId)
                .content(normalizeContent(content))
                .anonymous(anonymous)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    public InterviewComment delete(Instant deletedAt) {
        return InterviewComment.builder()
                .id(id)
                .interviewId(interviewId)
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
