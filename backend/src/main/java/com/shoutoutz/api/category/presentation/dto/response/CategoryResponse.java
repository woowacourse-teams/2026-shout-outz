package com.shoutoutz.api.category.presentation.dto.response;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryType;
import com.shoutoutz.api.feed.domain.FeedType;

public record CategoryResponse(
        long categoryId,
        String slug,
        String displayName,
        CategoryType type,
        FeedType feedType,
        int displayOrder,
        boolean active
) {

    public CategoryResponse(
            long categoryId,
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder,
            boolean active
    ) {
        this(categoryId, slug, displayName, type, FeedType.POST, displayOrder, active);
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getDisplayName(),
                category.getType(),
                category.getFeedType(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }
}
