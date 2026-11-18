package com.shoutoutz.api.resourcesearch.application.dto;

import java.util.List;

/**
 * 프로젝트 자료 한 페이지
 *
 * @param nextCursor 뒤에 자료가 더 있으면 이 페이지 마지막 자료의 위치, 없으면 null
 */
public record ProjectResourceSlice(
        List<ResourceCard> cards,
        ProjectResourceCursor nextCursor
) {

    public ProjectResourceSlice {
        cards = List.copyOf(cards);
    }

    /**
     * 한 페이지보다 하나 더 조회한 목록에서 페이지를 잘라 낸다. 더 조회된 자료가 있으면 다음 페이지가 있다.
     */
    public static ProjectResourceSlice of(List<ResourceCard> fetched, int size) {
        if (fetched.size() <= size) {
            return new ProjectResourceSlice(fetched, null);
        }
        List<ResourceCard> page = fetched.subList(0, size);
        return new ProjectResourceSlice(page, ProjectResourceCursor.after(page.getLast()));
    }
}
