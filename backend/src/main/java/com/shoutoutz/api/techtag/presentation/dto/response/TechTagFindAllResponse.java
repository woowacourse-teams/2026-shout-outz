package com.shoutoutz.api.techtag.presentation.dto.response;

import com.shoutoutz.api.techtag.domain.TechTag;
import java.util.List;

/**
 * 기술 스택 조회 응답
 * items 는 displayName 알파벳 오름차순이며, 전체 조회와 검색이 같은 구조를 사용한다.
 */
public record TechTagFindAllResponse(List<Item> items) {

    public static TechTagFindAllResponse from(List<TechTag> techTags) {
        return new TechTagFindAllResponse(techTags.stream()
                .map(Item::from)
                .toList());
    }

    public record Item(Long id, String displayName) {

        public static Item from(TechTag techTag) {
            return new Item(techTag.getId(), techTag.getDisplayName());
        }
    }
}
