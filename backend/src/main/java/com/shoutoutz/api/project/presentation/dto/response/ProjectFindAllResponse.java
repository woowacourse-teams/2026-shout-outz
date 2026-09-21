package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectSummary;
import java.net.URI;
import java.util.List;
import java.util.Map;

public record ProjectFindAllResponse(List<Item> items, Meta meta) {

    public ProjectFindAllResponse {
        items = List.copyOf(items);
    }

    public static ProjectFindAllResponse of(
            ProjectPage page,
            String nextCursor,
            Map<Long, URI> mediaUrls
    ) {
        return new ProjectFindAllResponse(
                page.items().stream().map(item -> Item.from(item, mediaUrls)).toList(),
                new Meta(nextCursor, page.hasNext(), page.totalCount())
        );
    }

    /**
     * 기술 스택과 팀원은 전체 목록을 등록 순서대로 내려준다. 카드에 몇 개까지 보여줄지는 화면에서 정한다.
     * starCount 는 GitHub 스타 수를 아직 동기화하지 않은 프로젝트면 null 이다.
     */
    public record Item(
            long id,
            String slug,
            String title,
            String tagline,
            int cohort,
            Long thumbnailMediaId,
            String thumbnailUrl,
            Integer starCount,
            long likeCount,
            long commentCount,
            List<ProjectTechTagResponse> techTags,
            List<ProjectMemberProfileResponse> members
    ) {

        public static Item from(ProjectSummary summary, Map<Long, URI> mediaUrls) {
            return new Item(
                    summary.id(),
                    summary.slug(),
                    summary.title(),
                    summary.tagline(),
                    summary.cohort(),
                    summary.thumbnailMediaId(),
                    toUrl(mediaUrls, summary.thumbnailMediaId()),
                    summary.starCount(),
                    summary.likeCount(),
                    summary.commentCount(),
                    summary.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                    summary.members().stream()
                            .map(member -> ProjectMemberProfileResponse.from(member, mediaUrls))
                            .toList()
            );
        }

        private static String toUrl(Map<Long, URI> mediaUrls, Long mediaId) {
            if (mediaId == null || mediaUrls == null) {
                return null;
            }
            URI url = mediaUrls.get(mediaId);
            return url == null ? null : url.toString();
        }
    }

    /**
     * @param nextCursor 다음 페이지 조회에 쓸 커서. 다음 페이지가 없으면 null이다.
     * @param totalCount 검색어와 필터가 적용된 전체 프로젝트 수
     */
    public record Meta(String nextCursor, boolean hasNext, long totalCount) {
    }
}
