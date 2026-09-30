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

public record FeedCommandResponse(
        long feedId,
        String title,
        String content,
        boolean isAnonymous,
        Author author,
        List<Category> categories,
        List<Media> media,
        LinkPreview linkPreview,
        Instant createdAt,
        Instant updatedAt
) {

    public static FeedCommandResponse from(FeedItem feed, Map<Long, URI> mediaUrls) {
        Map<Long, URI> urls = mediaUrls == null ? Map.of() : mediaUrls;
        return new FeedCommandResponse(
                feed.feedId(),
                feed.title(),
                feed.content(),
                feed.isAnonymous(),
                Author.from(feed.author(), urls),
                feed.categories().stream().map(Category::from).toList(),
                feed.media().stream().map(media -> Media.from(media, urls)).toList(),
                feed.linkPreview(),
                feed.createdAt(),
                feed.updatedAt()
        );
    }

    public record Author(
            Long userId,
            String handle,
            String displayName,
            UserType userType,
            String track,
            Short cohort,
            String avatarUrl
    ) {
        public Author(
                String handle,
                String displayName,
                UserType userType,
                String track,
                Short cohort,
                String avatarUrl
        ) {
            this(null, handle, displayName, userType, track, cohort, avatarUrl);
        }

        private static Author from(FeedItem.Author author, Map<Long, URI> mediaUrls) {
            return new Author(
                    author.userId(),
                    author.handle(),
                    author.displayName(),
                    author.userType(),
                    trackValue(author.userType(), author.track()),
                    cohortValue(author.userType(), author.cohort()),
                    toUrl(findUrl(mediaUrls, author.avatarImageId()))
            );
        }

        private static String trackValue(UserType userType, Track track) {
            return userType == UserType.WOOWACOURSE_CREW && track != null ? track.getValue() : null;
        }

        private static Short cohortValue(UserType userType, Cohort cohort) {
            return userType == UserType.WOOWACOURSE_CREW && cohort != null ? (short) cohort.getValue() : null;
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

    public record Media(String url, int displayOrder) {
        private static Media from(FeedItem.Media media, Map<Long, URI> mediaUrls) {
            return new Media(toUrl(findUrl(mediaUrls, media.mediaId())), media.displayOrder());
        }
    }

    private static URI findUrl(Map<Long, URI> mediaUrls, Long mediaId) {
        return mediaId == null ? null : mediaUrls.get(mediaId);
    }

    private static String toUrl(URI url) {
        return url == null ? null : url.toString();
    }
}
