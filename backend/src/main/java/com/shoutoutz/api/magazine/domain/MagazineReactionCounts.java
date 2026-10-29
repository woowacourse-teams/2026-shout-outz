package com.shoutoutz.api.magazine.domain;

public record MagazineReactionCounts(
        long likeCount,
        boolean likedByMe
) {
}
