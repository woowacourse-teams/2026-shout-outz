package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;

public record FeedResponse(
        long feedId,
        String content,
        Author author,
        List<Category> categories,
        List<Media> media,
        Instant createdAt,
        Instant updatedAt
) {
    public static FeedResponse from(FeedItem feed) {
        return new FeedResponse(
                feed.feedId(),
                feed.content(),
                Author.from(feed.author()),
                feed.categories().stream().map(Category::from).toList(),
                feed.media().stream().map(Media::from).toList(),
                feed.createdAt(),
                feed.updatedAt()
        );
    }

    public static List<FeedResponse> from(List<FeedItem> feeds) {
        return feeds.stream()
                .map(FeedResponse::from)
                .toList();
    }

    public record Author(
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            Long avatarImageId
    ) {
        private static Author from(FeedItem.Author author) {
            return new Author(
                    author.handle(),
                    author.displayName(),
                    author.userType(),
                    trackValue(author.track()),
                    cohortValue(author.cohort()),
                    author.avatarImageId()
            );
        }

        private static String trackValue(Track track) {
            if (track == null) {
                return null;
            }
            return track.getValue();
        }

        private static Short cohortValue(Cohort cohort) {
            if (cohort == null) {
                return null;
            }
            return (short) cohort.getValue();
        }
    }

    public record Category(
            long categoryId,
            String slug,
            String displayName,
            CategoryType type
    ) {
        private static Category from(FeedItem.Category category) {
            return new Category(
                    category.categoryId(),
                    category.slug(),
                    category.displayName(),
                    category.type()
            );
        }
    }

    public record Media(long mediaId, int displayOrder) {
        private static Media from(FeedItem.Media media) {
            return new Media(media.mediaId(), media.displayOrder());
        }
    }
}
