package com.shoutoutz.api.news.presentation.dto.response;

import java.time.Instant;

public record NewsDeleteResponse(
        Long id,
        Instant deletedAt
) {
}
