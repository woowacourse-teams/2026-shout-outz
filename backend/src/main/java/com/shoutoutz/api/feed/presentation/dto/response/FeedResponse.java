package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FeedResponse(
        long feedId,
        String title,
        String content,
        Author author,
        List<Category> categories,
        List<Media> media,
        LinkPreview linkPreview,
        long likeCount,
        long bookmarkCount,
        boolean likedByMe,
        boolean bookmarkedByMe,
        long commentCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static FeedResponse from(FeedItem feed) {
        return from(feed, Map.of());
    }

    public static FeedResponse from(FeedItem feed, Map<Long, URI> mediaUrls) {
        Map<Long, URI> urls = mediaUrls == null ? Map.of() : mediaUrls;
        return new FeedResponse(
                feed.feedId(),
                feed.title(),
                feed.content(),
                Author.from(feed.author(), urls),
                feed.categories().stream().map(Category::from).toList(),
                feed.media().stream().map(media -> Media.from(media, urls)).toList(),
                feed.linkPreview(),
                feed.likeCount(),
                feed.bookmarkCount(),
                feed.likedByMe(),
                feed.bookmarkedByMe(),
                feed.commentCount(),
                feed.createdAt(),
                feed.updatedAt()
        );
    }

    public static List<FeedResponse> from(List<FeedItem> feeds) {
        return from(feeds, Map.of());
    }

    public static List<FeedResponse> from(List<FeedItem> feeds, Map<Long, URI> mediaUrls) {
        return feeds.stream()
                .map(feed -> from(feed, mediaUrls))
                .toList();
    }

    public record Author(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            Long avatarImageId,
            String avatarUrl
    ) {
        public Author(
                String handle,
                String displayName,
                UserType userType,
                String track,
                Short cohort,
                Long avatarImageId,
                String avatarUrl
        ) {
            this(null, handle, displayName, userType, track, cohort, avatarImageId, avatarUrl);
        }

        private static Author from(FeedItem.Author author, Map<Long, URI> mediaUrls) {
            return new Author(
                    author.userId(),
                    author.handle(),
                    author.displayName(),
                    author.userType(),
                    trackValue(author.userType(), author.track()),
                    cohortValue(author.userType(), author.cohort()),
                    author.avatarImageId(),
                    toUrl(findUrl(mediaUrls, author.avatarImageId()))
            );
        }

        private static String trackValue(UserType userType, Track track) {
            if (userType != UserType.WOOWACOURSE_CREW || track == null) {
                return null;
            }
            return track.getValue();
        }

        private static Short cohortValue(UserType userType, Cohort cohort) {
            if (userType != UserType.WOOWACOURSE_CREW || cohort == null) {
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

    public record Media(long mediaId, String url, int displayOrder) {
        private static Media from(FeedItem.Media media, Map<Long, URI> mediaUrls) {
            return new Media(
                    media.mediaId(),
                    toUrl(findUrl(mediaUrls, media.mediaId())),
                    media.displayOrder()
            );
        }
    }

    private static URI findUrl(Map<Long, URI> mediaUrls, Long mediaId) {
        return mediaId == null ? null : mediaUrls.get(mediaId);
    }

    private static String toUrl(URI url) {
        return url == null ? null : url.toString();
    }
}
