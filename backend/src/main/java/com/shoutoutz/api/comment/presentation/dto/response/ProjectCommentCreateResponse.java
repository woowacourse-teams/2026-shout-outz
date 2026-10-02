package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import java.time.Instant;

public record ProjectCommentCreateResponse(
        Long id,
        String content,
        Author author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        LinkPreview linkPreview
) {

    public ProjectCommentCreateResponse(
            Long id,
            String content,
            Author author,
            Long parentId,
            Instant createdAt,
            Instant updatedAt,
            boolean editable
    ) {
        this(id, content, author, parentId, createdAt, updatedAt, editable, null);
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
