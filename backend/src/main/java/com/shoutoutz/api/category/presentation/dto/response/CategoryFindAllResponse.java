package com.shoutoutz.api.category.presentation.dto.response;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.feed.domain.FeedType;
import java.util.List;

public record CategoryFindAllResponse(
        long categoryId,
        String slug,
        String displayName,
        CategoryType type,
        FeedType feedType,
        int displayOrder
) {

    public CategoryFindAllResponse(
            long categoryId,
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder
    ) {
        this(categoryId, slug, displayName, type, FeedType.POST, displayOrder);
    }

    public static List<CategoryFindAllResponse> from(List<Category> categories) {
        return categories.stream()
                .map(CategoryFindAllResponse::from)
                .toList();
    }

    private static CategoryFindAllResponse from(Category category) {
        return new CategoryFindAllResponse(
                category.getId(),
                category.getSlug(),
                category.getDisplayName(),
                category.getType(),
                category.getFeedType(),
                category.getDisplayOrder()
        );
    }
}
