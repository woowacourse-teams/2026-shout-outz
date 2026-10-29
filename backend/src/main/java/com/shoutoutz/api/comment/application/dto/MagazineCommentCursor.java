package com.shoutoutz.api.comment.application.dto;

import com.shoutoutz.api.comment.domain.MagazineCommentSort;
import java.time.Instant;

public record MagazineCommentCursor(
        Instant createdAt,
        long id,
        MagazineCommentSort sort
) {
}
