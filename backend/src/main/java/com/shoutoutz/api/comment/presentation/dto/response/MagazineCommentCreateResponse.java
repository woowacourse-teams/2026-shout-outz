package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import java.time.Instant;

public record MagazineCommentCreateResponse(
        Long id,
        String content,
        MagazineCommentAuthorResponse author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        boolean isAnonymous,
        LinkPreview linkPreview
) {
    public static MagazineCommentCreateResponse from(
            MagazineComment comment,
            UserProfile author,
            String handle,
            String avatarUrl,
            LinkPreview linkPreview
    ) {
        return new MagazineCommentCreateResponse(
                comment.getId(),
                comment.getContent(),
                MagazineCommentAuthorResponse.from(author, handle, avatarUrl),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                true,
                comment.isAnonymous(),
                linkPreview
        );
    }
}
