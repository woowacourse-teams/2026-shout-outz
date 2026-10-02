package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;

/**
 * 소식 상세 조회 응답 객체
 */
public record NewsFindResponse(
        long id,
        NewsType type,
        String title,
        String body,
        Author author,
        Instant publishedAt,
        EventStatus eventStatus,
        Instant eventStartAt,
        Instant eventEndAt,
        boolean isPinned,
        Integer pinOrder,
        long likeCount,
        boolean likedByMe,
        Cta cta,
        Navigation previous,
        Navigation next
) {

    public NewsFindResponse(
            long id,
            NewsType type,
            String title,
            String body,
            Author author,
            Instant publishedAt,
            EventStatus eventStatus,
            Instant eventStartAt,
            Instant eventEndAt,
            boolean isPinned,
            Integer pinOrder,
            Cta cta,
            Navigation previous,
            Navigation next
    ) {
        this(
                id,
                type,
                title,
                body,
                author,
                publishedAt,
                eventStatus,
                eventStartAt,
                eventEndAt,
                isPinned,
                pinOrder,
                0L,
                false,
                cta,
                previous,
                next
        );
    }

    public record Author(
            long userId,
            String name,
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort
    ) {
        public Author(long userId, String name, String handle) {
            this(userId, name, handle, name, null, null, null);
        }

        public Author(long userId, String name) {
            this(userId, name, null, name, null, null, null);
        }

        public Author(long userId, String name, String displayName, UserType userType, String track, Short cohort) {
            this(userId, name, null, displayName, userType, track, cohort);
        }
    }

    public record Cta(String label, String url) {
    }

    /**
     * 이전/다음글 이동 관련 데이터
     */
    public record Navigation(long id, String title, Instant publishedAt) {
    }
}
