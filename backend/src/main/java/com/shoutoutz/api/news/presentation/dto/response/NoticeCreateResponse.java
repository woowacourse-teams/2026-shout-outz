package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.enums.NewsType;
import java.time.Instant;

public record NoticeCreateResponse(
        long id,
        NewsType type,
        String title,
        String summary,
        String body,
        Author author,
        Instant publishedAt,
        boolean isPinned,
        Integer pinOrder,
        Cta cta
) {

    public record Author(long userId, String name) {
    }

    public record Cta(String label, String url) {
    }
}
