package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectReactionType;

public record ProjectReactionResponse(
        long projectId,
        ProjectReactionType type,
        boolean active,
        long likeCount,
        long bookmarkCount
) {
}
