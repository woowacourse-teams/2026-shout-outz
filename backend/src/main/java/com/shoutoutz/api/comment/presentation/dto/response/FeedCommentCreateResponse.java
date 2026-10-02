package com.shoutoutz.api.comment.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
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
        boolean isAnonymous,
        LinkPreview linkPreview
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
        this(id, content, author, parentId, createdAt, updatedAt, editable, false, null);
    }

    public FeedCommentCreateResponse(
            Long id,
            String content,
            Author author,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            boolean editable,
            boolean isAnonymous
    ) {
        this(id, content, author, parentId, createdAt, updatedAt, editable, isAnonymous, null);
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
