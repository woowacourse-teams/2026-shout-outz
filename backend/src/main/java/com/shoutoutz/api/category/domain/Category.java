package com.shoutoutz.api.category.domain;

import com.shoutoutz.api.feed.domain.FeedType;
import lombok.Getter;

/**
 * 피드 분류와 이벤트 표시 정보.
 */
@Getter
public final class Category {

    private final Long id;
    private final String slug;
    private final String displayName;
    private final CategoryType type;
    /**
     * 이 카테고리가 사용될 피드 유형. categories 테이블이 아니라 매핑 테이블의 값이다.
     */
    private final FeedType feedType;
    private final int displayOrder;
    private final boolean active;

    private Category(
            Long id,
            String slug,
            String displayName,
            CategoryType type,
            FeedType feedType,
            int displayOrder,
            boolean active
    ) {
        CategoryValidator.validate(id, slug, displayName, type, feedType, displayOrder);
        this.id = id;
        this.slug = slug;
        this.displayName = displayName;
        this.type = type;
        this.feedType = feedType;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static Category create(
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder
    ) {
        return create(slug, displayName, type, FeedType.POST, displayOrder);
    }

    public static Category create(
            String slug,
            String displayName,
            CategoryType type,
            FeedType feedType,
            int displayOrder
    ) {
        return new Category(null, slug, displayName, type, feedType, displayOrder, true);
    }

    public static Category reconstitute(
            long id,
            String slug,
            String displayName,
            CategoryType type,
            int displayOrder,
            boolean active
    ) {
        return reconstitute(id, slug, displayName, type, FeedType.POST, displayOrder, active);
    }

    public static Category reconstitute(
            long id,
            String slug,
            String displayName,
            CategoryType type,
            FeedType feedType,
            int displayOrder,
            boolean active
    ) {
        return new Category(id, slug, displayName, type, feedType, displayOrder, active);
    }

    public Category update(String displayName, int displayOrder) {
        return new Category(id, slug, displayName, type, feedType, displayOrder, active);
    }

    public Category deactivate() {
        return new Category(id, slug, displayName, type, feedType, displayOrder, false);
    }

    public boolean isGeneral() {
        return type == CategoryType.GENERAL;
    }
}
