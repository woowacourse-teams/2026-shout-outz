package com.shoutoutz.api.feed.presentation.dto.response;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.cohort.domain.Cohort;
import com.shoutoutz.api.feed.application.dto.FeedItem;
import com.shoutoutz.api.feed.application.dto.LinkPreview;
import com.shoutoutz.api.feed.domain.FeedType;
import com.shoutoutz.api.user.domain.profile.Track;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FeedResponse(
        long feedId,
        FeedType feedType,
        String title,
        String content,
        boolean isAnonymous,
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
        return from(feed, mediaUrls, Map.of());
    }

    public static FeedResponse from(
            FeedItem feed,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        Map<Long, URI> urls = mediaUrls == null ? Map.of() : mediaUrls;
        Map<Long, String> fallbackUrls = userAvatarUrls == null ? Map.of() : userAvatarUrls;
        return new FeedResponse(
                feed.feedId(),
                feed.feedType(),
                feed.title(),
                feed.content(),
                feed.isAnonymous(),
                Author.from(feed.author(), urls, fallbackUrls),
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
        return from(feeds, mediaUrls, Map.of());
    }

    public static List<FeedResponse> from(
            List<FeedItem> feeds,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls
    ) {
        return feeds.stream()
                .map(feed -> from(feed, mediaUrls, userAvatarUrls))
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

        private static Author from(
                FeedItem.Author author,
                Map<Long, URI> mediaUrls,
                Map<Long, String> userAvatarUrls
        ) {
            return new Author(
                    author.userId(),
                    author.handle(),
                    author.displayName(),
                    author.userType(),
                    trackValue(author.userType(), author.track()),
                    cohortValue(author.userType(), author.cohort()),
                    author.avatarImageId(),
                    avatarUrl(author, mediaUrls, userAvatarUrls)
            );
        }

        private static String avatarUrl(
                FeedItem.Author author,
                Map<Long, URI> mediaUrls,
                Map<Long, String> userAvatarUrls
        ) {
            URI mediaUrl = findUrl(mediaUrls, author.avatarImageId());
            if (mediaUrl != null) {
                return mediaUrl.toString();
            }
            return author.userId() == null ? null : userAvatarUrls.get(author.userId());
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
            FeedType feedType,
            CategoryType type
    ) {
        public Category(
                long categoryId,
                String slug,
                String displayName,
                CategoryType type
        ) {
            this(categoryId, slug, displayName, FeedType.POST, type);
        }

        private static Category from(FeedItem.Category category) {
            return new Category(
                    category.categoryId(),
                    category.slug(),
                    category.displayName(),
                    category.feedType(),
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
