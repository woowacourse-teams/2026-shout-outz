package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectReactionType;

public record ProjectReactionResponse(
        String slug,
        ProjectReactionType type,
        boolean active,
        long likeCount,
        long bookmarkCount
) {
}
