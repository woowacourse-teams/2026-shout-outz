package com.shoutoutz.api.comment.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;

public record FeedCommentUpdateResponse(
        Long id,
        String content,
        Author author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        boolean edited,
        boolean isAnonymous
) {

    public FeedCommentUpdateResponse(
            Long id,
            String content,
            Author author,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            boolean editable,
            boolean edited
    ) {
        this(id, content, author, parentId, createdAt, updatedAt, editable, edited, false);
    }

    public record Author(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            String track,
            @JsonInclude(JsonInclude.Include.NON_NULL) Short cohort,
            Boolean isCurrent,
            String avatarUrl
    ) {
        public Author(Long userId, String handle, String displayName, String avatarUrl) {
            this(userId, handle, displayName, null, null, null, null, avatarUrl);
        }

        public Author(Long userId, String displayName, String avatarUrl) {
            this(userId, null, displayName, null, null, null, null, avatarUrl);
        }
    }
}
