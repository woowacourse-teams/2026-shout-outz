package com.shoutoutz.api.post.presentation.dto.response;

import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.post.application.dto.PostItem;
import com.shoutoutz.api.user.domain.profile.UserType;
import java.time.Instant;
import java.util.List;

public record PostResponse(
        long postId,
        String content,
        Author author,
        List<Category> categories,
        List<Media> media,
        Instant createdAt,
        Instant updatedAt
) {
    public static PostResponse from(PostItem post) {
        return new PostResponse(
                post.postId(),
                post.content(),
                Author.from(post.author()),
                post.categories().stream().map(Category::from).toList(),
                post.media().stream().map(Media::from).toList(),
                post.createdAt(),
                post.updatedAt()
        );
    }

    public static List<PostResponse> from(List<PostItem> posts) {
        return posts.stream()
                .map(PostResponse::from)
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
        private static Author from(PostItem.Author author) {
            return new Author(
                    author.handle(),
                    author.displayName(),
                    author.userType(),
                    author.track(),
                    author.cohort(),
                    author.avatarImageId()
            );
        }
    }

    public record Category(
            long categoryId,
            String slug,
            String displayName,
            CategoryType type
    ) {
        private static Category from(PostItem.Category category) {
            return new Category(
                    category.categoryId(),
                    category.slug(),
                    category.displayName(),
                    category.type()
            );
        }
    }

    public record Media(long mediaId, int displayOrder) {
        private static Media from(PostItem.Media media) {
            return new Media(media.mediaId(), media.displayOrder());
        }
    }
}
