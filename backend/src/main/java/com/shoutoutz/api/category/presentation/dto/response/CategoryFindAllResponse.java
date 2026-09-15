package com.shoutoutz.api.category.presentation.dto.response;

import com.shoutoutz.api.category.domain.Category;
import com.shoutoutz.api.category.domain.CategoryType;
import java.util.List;

public record CategoryFindAllResponse(
        long categoryId,
        String slug,
        String displayName,
        CategoryType type,
        int displayOrder
) {

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
                category.getDisplayOrder()
        );
    }
}
