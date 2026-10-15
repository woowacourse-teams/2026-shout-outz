package com.shoutoutz.api.comment.infrastructure.mapper;

import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.comment.infrastructure.InterviewCommentEntity;

public final class InterviewCommentMapper {

    private InterviewCommentMapper() {
    }

    public static InterviewCommentEntity toEntity(InterviewComment comment) {
        return InterviewCommentEntity.builder()
                .id(comment.getId())
                .interviewId(comment.getInterviewId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .anonymous(comment.isAnonymous())
                .deletedAt(comment.getDeletedAt())
                .build();
    }

    public static InterviewComment toDomain(InterviewCommentEntity entity) {
        return InterviewComment.reconstitute(
                entity.getId(),
                entity.getInterviewId(),
                entity.getAuthorId(),
                entity.getParentId(),
                entity.getContent(),
                entity.isAnonymous(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
