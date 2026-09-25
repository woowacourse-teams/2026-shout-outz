package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.NewsReactionType;

public record NewsReactionResponse(
        long newsId,
        NewsReactionType type,
        boolean active,
        long likeCount
) {
}
