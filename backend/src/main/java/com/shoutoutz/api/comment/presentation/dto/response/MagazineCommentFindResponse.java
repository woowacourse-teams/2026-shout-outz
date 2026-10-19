package com.shoutoutz.api.comment.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shoutoutz.api.comment.domain.MagazineComment;
import com.shoutoutz.api.comment.domain.MagazineCommentReactionCounts;
import com.shoutoutz.api.common.response.SliceMetaResponse;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record MagazineCommentFindResponse(
        List<Comment> comments,
        SliceMetaResponse meta
) {
    public MagazineCommentFindResponse {
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
            boolean isAnonymous,
            LinkPreview linkPreview
    ) {
        public static Comment from(
                MagazineComment comment,
                UserProfile author,
                String handle,
                String avatarUrl,
                Long viewerId,
                MagazineCommentReactionCounts reactions,
                LinkPreview preview
        ) {
            boolean owner = Objects.equals(comment.getAuthorId(), viewerId);
            boolean authorVisible = !comment.isAnonymous() || owner;
            String content = comment.getContent();
            if (comment.isDeleted()) {
                content = null;
                reactions = new MagazineCommentReactionCounts(0L, false);
                preview = null;
            }
            return new Comment(
                    comment.getId(), content, Author.from(author, handle, avatarUrl, authorVisible),
                    comment.getParentId(), comment.getCreatedAt(), comment.getUpdatedAt(),
                    !comment.isDeleted() && owner, !comment.isDeleted() && comment.isEdited(), comment.isDeleted(),
                    reactions.agreeCount(), reactions.agreedByMe(), comment.isAnonymous(), preview
            );
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
        private static Author from(UserProfile profile, String handle, String avatarUrl, boolean visible) {
            MagazineCommentAuthorResponse author = MagazineCommentAuthorResponse.from(profile, handle, avatarUrl);
            if (!visible) {
                return new Author(null, null, null, author.userType(), null, null, author.isCurrent(), null, null);
            }
            return new Author(
                    author.userId(), author.handle(), author.displayName(), author.userType(),
                    author.track(), author.cohort(), author.isCurrent(), profile.getAvatarImageId(), author.avatarUrl()
            );
        }
    }
}
