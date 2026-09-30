package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;

public record FeedCommentCreateResponse(
        Long id,
        String content,
        Author author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        boolean isAnonymous
) {

    public FeedCommentCreateResponse(
            Long id,
            String content,
            Author author,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            boolean editable
    ) {
        this(id, content, author, parentId, createdAt, updatedAt, editable, false);
    }

    public record Author(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String avatarUrl
    ) {
        public Author(Long userId, String handle, String displayName, String avatarUrl) {
            this(userId, handle, displayName, null, null, null, avatarUrl);
        }

        public Author(Long userId, String displayName, String avatarUrl) {
            this(userId, null, displayName, null, null, null, avatarUrl);
        }
    }
}
