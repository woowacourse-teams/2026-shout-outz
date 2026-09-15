package com.shoutoutz.api.category.presentation.dto.response;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryType;

public record CategoryResponse(
        long categoryId,
        String slug,
        String displayName,
        CategoryType type,
        int displayOrder,
        boolean active
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getDisplayName(),
                category.getType(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }
}
