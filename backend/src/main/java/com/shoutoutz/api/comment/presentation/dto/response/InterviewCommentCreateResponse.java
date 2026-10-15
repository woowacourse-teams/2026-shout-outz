package com.shoutoutz.api.comment.presentation.dto.response;

import com.shoutoutz.api.comment.domain.InterviewComment;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import java.time.Instant;

public record InterviewCommentCreateResponse(
        Long id,
        String content,
        InterviewCommentAuthorResponse author,
        Long parentId,
        Instant createdAt,
        Instant updatedAt,
        boolean editable,
        boolean isAnonymous,
        LinkPreview linkPreview
) {
    public static InterviewCommentCreateResponse from(
            InterviewComment comment,
            UserProfile author,
            String handle,
            String avatarUrl,
            LinkPreview linkPreview
    ) {
        return new InterviewCommentCreateResponse(
                comment.getId(),
                comment.getContent(),
                InterviewCommentAuthorResponse.from(author, handle, avatarUrl),
                comment.getParentId(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                true,
                comment.isAnonymous(),
                linkPreview
        );
    }
}
