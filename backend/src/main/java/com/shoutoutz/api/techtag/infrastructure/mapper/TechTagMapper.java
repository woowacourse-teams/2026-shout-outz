package com.shoutoutz.api.techtag.infrastructure.mapper;

import com.shoutoutz.api.techtag.domain.TechTag;
import com.shoutoutz.api.techtag.infrastructure.TechTagEntity;
import java.util.List;

/**
 * 기술 태그는 마이그레이션으로만 등록되고 애플리케이션이 생성하지 않으므로, 도메인 -> 엔티티 변환은 두지 않는다.
 */
public final class TechTagMapper {

    private TechTagMapper() {
    }

    public static TechTag toDomain(TechTagEntity entity) {
        return TechTag.builder()
                .id(entity.getId())
                .slug(entity.getSlug())
                .displayName(entity.getDisplayName())
                .active(entity.isActive())
                .build();
    }

    public static List<TechTag> toDomains(List<TechTagEntity> entities) {
        return entities.stream()
                .map(TechTagMapper::toDomain)
                .toList();
    }
}
