package com.shoutoutz.api.news.presentation.dto.response;

import com.shoutoutz.api.news.domain.News;
import com.shoutoutz.api.news.domain.NewsCta;
import com.shoutoutz.api.news.domain.enums.EventStatus;
import com.shoutoutz.api.news.domain.enums.NewsType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserProfile;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;

public record NewsUpdateResponse(
        long id,
        NewsType type,
        String title,
        String summary,
        String body,
        Author author,
        Instant publishedAt,
        EventStatus eventStatus,
        Instant eventStartAt,
        Instant eventEndAt,
        boolean isPinned,
        Integer pinOrder,
        Cta cta
) {

    public static NewsUpdateResponse from(News news, Instant now) {
        return from(news, now, null);
    }

    public static NewsUpdateResponse from(News news, Instant now, UserProfile authorProfile) {
        EventStatus eventStatus = news.getType() == NewsType.EVENT
                ? news.eventStatusAt(now)
                : null;
        UserType userType = authorProfile == null ? null : authorProfile.getUserType();
        String displayName = authorProfile == null
                ? news.getAuthorName()
                : authorProfile.getDisplayName().value();
        return new NewsUpdateResponse(
                news.getId(),
                news.getType(),
                news.getTitle(),
                news.getSummary(),
                news.getBody(),
                new Author(
                        news.getAuthorId(),
                        news.getAuthorName(),
                        displayName,
                        userType,
                        trackValue(authorProfile),
                        cohortValue(authorProfile)
                ),
                news.getPublishedAt(),
                eventStatus,
                news.getEventStartAt(),
                news.getEventEndAt(),
                news.isPinned(),
                news.getPinOrder(),
                Cta.from(news.getCta())
        );
    }

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

        private static Cta from(NewsCta cta) {
            return cta == null ? null : new Cta(cta.label(), cta.url());
        }
    }

    private static String trackValue(UserProfile profile) {
        if (profile == null || profile.getUserType() != UserType.WOOWACOURSE_CREW
                || profile.getTrack() == null) {
            return null;
        }
        Track track = profile.getTrack();
        return track.getValue();
    }

    private static Short cohortValue(UserProfile profile) {
        if (profile == null || profile.getUserType() != UserType.WOOWACOURSE_CREW
                || profile.getCohort() == null) {
            return null;
        }
        Cohort cohort = profile.getCohort();
        return (short) cohort.getValue();
    }
}
