package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;

public record ProjectCommentCreateResponse(
        Long id,
        String content,
        Author author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable
) {

    public record Author(
            Long userId,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String avatarUrl
    ) {
        public Author(Long userId, String displayName, String avatarUrl) {
            this(userId, displayName, null, null, null, avatarUrl);
        }
    }
}
