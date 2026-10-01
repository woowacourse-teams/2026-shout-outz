package com.shoutoutz.api.comment.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;

public record FeedCommentFindResponse(
        List<Comment> comments,
        SliceMetaResponse meta
) {

    public FeedCommentFindResponse {
        comments = List.copyOf(comments);
    }

    public record Comment(
            Long id,
            String content,
            Author author,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            boolean editable,
            boolean edited,
            boolean deleted,
            long agreeCount,
            boolean agreedByMe,
            boolean isAnonymous
    ) {

        public Comment(
                Long id,
                String content,
                Author author,
                Long parentId,
                Instant createdAt,
                Instant updatedAt,
                boolean editable,
                boolean edited,
                boolean deleted
        ) {
            this(id, content, author, parentId, createdAt, updatedAt, editable, edited, deleted, 0L, false, false);
        }

        public Comment(
                Long id,
                String content,
                Author author,
                Long parentId,
                Instant createdAt,
                Instant updatedAt,
                boolean editable,
                boolean edited,
                boolean deleted,
                long agreeCount,
                boolean agreedByMe
        ) {
            this(id, content, author, parentId, createdAt, updatedAt, editable, edited, deleted,
                    agreeCount, agreedByMe, false);
        }
    }

    public record Author(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            String track,
            @JsonInclude(JsonInclude.Include.NON_NULL) Short cohort,
            Boolean isCurrent,
            Long avatarImageId,
            String avatarUrl
    ) {
        public Author(Long userId, String handle, String displayName, Long avatarImageId, String avatarUrl) {
            this(userId, handle, displayName, null, null, null, null, avatarImageId, avatarUrl);
        }

        public Author(
                Long userId,
                String displayName,
                Long avatarImageId,
                String avatarUrl
        ) {
            this(userId, null, displayName, null, null, null, null, avatarImageId, avatarUrl);
        }
    }
}
