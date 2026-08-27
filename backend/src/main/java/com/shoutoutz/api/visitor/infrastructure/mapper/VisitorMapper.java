package com.shoutoutz.api.visitor.infrastructure.mapper;

import com.shoutoutz.api.visitor.domain.Visitor;
import com.shoutoutz.api.visitor.infrastructure.VisitorEntity;

public class VisitorMapper {

    private VisitorMapper() {
    }

    public static VisitorEntity toEntity(Visitor visitor) {
        return VisitorEntity.builder()
                .id(visitor.getId())
                .example(visitor.getExample())
                .build();
    }

    public static Visitor toDomain(VisitorEntity entity) {
        return Visitor.builder()
                .id(entity.getId())
                .example(entity.getExample())
                .build();
    }
}
