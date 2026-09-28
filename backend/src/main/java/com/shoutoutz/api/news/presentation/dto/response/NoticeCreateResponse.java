package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.user.domain.profile.UserType;
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

    public record Author(
            long userId,
            String name,
            String displayName,
            UserType userType,
            String track,
            Short cohort
    ) {
        public Author(long userId, String name) {
            this(userId, name, name, null, null, null);
        }
    }

    public record Cta(String label, String url) {
    }
}
