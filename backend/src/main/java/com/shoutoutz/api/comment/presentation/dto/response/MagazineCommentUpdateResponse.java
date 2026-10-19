package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import java.time.Instant;

public record MagazineCommentUpdateResponse(
        Long id,
        String content,
        MagazineCommentAuthorResponse author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        boolean edited,
        boolean isAnonymous,
        LinkPreview linkPreview
) {
    public static MagazineCommentUpdateResponse from(
            MagazineComment comment,
            UserProfile author,
            String handle,
            String avatarUrl,
            LinkPreview linkPreview
    ) {
        return new MagazineCommentUpdateResponse(
                comment.getId(),
                comment.getContent(),
                MagazineCommentAuthorResponse.from(author, handle, avatarUrl),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                true,
                comment.isEdited(),
                comment.isAnonymous(),
                linkPreview
        );
    }
}
