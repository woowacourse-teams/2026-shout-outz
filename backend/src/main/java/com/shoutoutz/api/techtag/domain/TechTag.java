package com.shoutoutz.api.techtag.domain;

import lombok.Builder;
import lombok.Getter;

/**
 * 프로젝트에 붙일 수 있는 기술 스택 태그
 * 관리자가 마이그레이션으로 미리 등록해 두고, 사용자는 검색해서 선택만 한다.
 * 별도 검증을 두지 않는 이유는 사용자 입력으로 생성되지 않고,
 * DB 제약(NOT NULL, UNIQUE)을 통과한 행에서만 매퍼가 생성하기 때문이다.
 */
@Getter
public class TechTag {

    private final Long id;
    private final String slug;
    private final String displayName;
    private final boolean active;

    @Builder
    private TechTag(
            Long id,
            String slug,
            String displayName,
            boolean active
    ) {
        this.id = id;
        this.slug = slug;
        this.displayName = displayName;
        this.active = active;
    }
}
