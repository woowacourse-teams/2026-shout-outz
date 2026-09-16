package com.shoutoutz.api.comment.infrastructure.mapper;

import com.shoutoutz.api.comment.domain.FeedComment;
import com.shoutoutz.api.comment.infrastructure.FeedCommentEntity;

public final class FeedCommentMapper {

    private FeedCommentMapper() {
    }

    public static FeedCommentEntity toEntity(FeedComment comment) {
        return FeedCommentEntity.builder()
                .id(comment.getId())
                .feedId(comment.getFeedId())
                .authorId(comment.getAuthorId())
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .deletedAt(comment.getDeletedAt())
                .build();
    }

    public static FeedComment toDomain(FeedCommentEntity entity) {
        return FeedComment.reconstitute(
                entity.getId(),
                entity.getFeedId(),
                entity.getAuthorId(),
                entity.getParentId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
