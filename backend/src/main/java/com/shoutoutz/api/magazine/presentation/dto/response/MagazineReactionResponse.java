package com.shoutoutz.api.magazine.presentation.dto.response;

import com.shoutoutz.api.magazine.domain.MagazineReactionType;

public record MagazineReactionResponse(
        long magazineId,
        MagazineReactionType type,
        boolean active,
        long likeCount
) {
}
