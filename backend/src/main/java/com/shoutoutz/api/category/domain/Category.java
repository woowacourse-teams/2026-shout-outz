package com.shoutoutz.api.category.domain;

import lombok.Getter;

/**
 * 포스트 분류와 이벤트 표시 정보.
 */
@Getter
public final class Category {

    private final Long id;
    private final String slug;
    private final String displayName;
    private final CategoryType type;
    private final int displayOrder;
    private final boolean active;

    private Category(
            Long id,
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder,
            boolean active
    ) {
        CategoryValidator.validate(id, slug, displayName, type, displayOrder);
        this.id = id;
        this.slug = slug;
        this.displayName = displayName;
        this.type = type;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static Category create(
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder
    ) {
        return new Category(null, slug, displayName, type, displayOrder, true);
    }

    public static Category reconstitute(
            long id,
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder,
            boolean active
    ) {
        return new Category(id, slug, displayName, type, displayOrder, active);
    }

    public Category update(String displayName, int displayOrder) {
        return new Category(id, slug, displayName, type, displayOrder, active);
    }

    public Category deactivate() {
        return new Category(id, slug, displayName, type, displayOrder, false);
    }

    public boolean isGeneral() {
        return type == CategoryType.GENERAL;
    }
}
