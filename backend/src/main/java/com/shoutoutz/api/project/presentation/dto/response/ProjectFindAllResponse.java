package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectSummary;
import java.util.List;

public record ProjectFindAllResponse(List<Item> items, Meta meta) {

    public ProjectFindAllResponse {
        items = List.copyOf(items);
    }

    public static ProjectFindAllResponse of(ProjectPage page, String nextCursor) {
        return new ProjectFindAllResponse(
                page.items().stream().map(Item::from).toList(),
                new Meta(nextCursor, page.hasNext(), page.totalCount())
        );
    }

    public record Item(
            long id,
            String slug,
            String title,
            String tagline,
            int cohort,
            Long thumbnailMediaId,
            long likeCount,
            long commentCount
    ) {

        private static Item from(ProjectSummary summary) {
            return new Item(
                    summary.id(),
                    summary.slug(),
                    summary.title(),
                    summary.tagline(),
                    summary.cohort(),
                    summary.thumbnailMediaId(),
                    summary.likeCount(),
                    summary.commentCount()
            );
        }
    }

    /**
     * @param nextCursor 다음 페이지 조회에 쓸 커서. 다음 페이지가 없으면 null이다.
     * @param totalCount 검색어와 필터가 적용된 전체 프로젝트 수
     */
    public record Meta(String nextCursor, boolean hasNext, long totalCount) {
    }
}
