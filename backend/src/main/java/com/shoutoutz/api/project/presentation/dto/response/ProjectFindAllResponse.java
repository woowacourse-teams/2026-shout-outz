package com.shoutoutz.api.project.presentation.dto.response;

import com.shoutoutz.api.project.domain.ProjectPage;
import com.shoutoutz.api.project.domain.ProjectSummary;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
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
        return of(page, nextCursor, mediaUrls, Map.of(), null);
    }

    public static ProjectFindAllResponse of(
            ProjectPage page,
            String nextCursor,
            Map<Long, URI> mediaUrls,
            Map<Long, String> userAvatarUrls,
            ResourceTopic matchedTopic
    ) {
        return new ProjectFindAllResponse(
                page.items().stream()
                        .map(item -> Item.from(item, mediaUrls, userAvatarUrls))
                        .toList(),
                new Meta(nextCursor, page.hasNext(), page.totalCount(), MatchedTopic.from(matchedTopic))
        );
    }

    /**
     * 목록 공통 페이지 정보에 검색어 해석 결과를 더한 메타 정보
     *
     * @param matchedTopic 검색어를 기술 주제로 해석해 그 주제로 조회했을 때의 주제. 아니면 null
     */
    public record Meta(
            String nextCursor,
            boolean hasNext,
            long totalCount,
            MatchedTopic matchedTopic
    ) {
    }

    public record MatchedTopic(
            String topicId,
            String name,
            String side
    ) {

        public static MatchedTopic from(ResourceTopic topic) {
            if (topic == null) {
                return null;
            }
            return new MatchedTopic(topic.topicId(), topic.name(), topic.side());
        }
    }

    /**
     * 기술 스택과 팀원은 전체 목록을 등록 순서대로 내려준다. 카드에 몇 개까지 보여줄지는 화면에서 정한다.
     * starCount 는 GitHub 스타 수를 아직 동기화하지 않은 프로젝트면 null 이다.
     */
    public record Item(
            String slug,
            String title,
            String tagline,
            int cohort,
            Long thumbnailImageId,
            String thumbnailUrl,
            Integer starCount,
            long likeCount,
            long commentCount,
            long bookmarkCount,
            boolean likedByMe,
            boolean bookmarkedByMe,
            List<ProjectTechTagResponse> techTags,
            List<ProjectMemberProfileResponse> members
    ) {

        public Item(
                String slug,
                String title,
                String tagline,
                int cohort,
                Long thumbnailImageId,
                String thumbnailUrl,
                Integer starCount,
                long likeCount,
                long commentCount,
                List<ProjectTechTagResponse> techTags,
                List<ProjectMemberProfileResponse> members
        ) {
            this(
                    slug,
                    title,
                    tagline,
                    cohort,
                    thumbnailImageId,
                    thumbnailUrl,
                    starCount,
                    likeCount,
                    commentCount,
                    0L,
                    false,
                    false,
                    techTags,
                    members
            );
        }

        public static Item from(ProjectSummary summary, Map<Long, URI> mediaUrls) {
            return from(summary, mediaUrls, Map.of());
        }

        public static Item from(
                ProjectSummary summary,
                Map<Long, URI> mediaUrls,
                Map<Long, String> userAvatarUrls
        ) {
            return new Item(
                    summary.slug(),
                    summary.title(),
                    summary.tagline(),
                    summary.cohort(),
                    summary.thumbnailMediaId(),
                    toUrl(mediaUrls, summary.thumbnailMediaId()),
                    summary.starCount(),
                    summary.likeCount(),
                    summary.commentCount(),
                    summary.bookmarkCount(),
                    summary.likedByMe(),
                    summary.bookmarkedByMe(),
                    summary.techTags().stream().map(ProjectTechTagResponse::from).toList(),
                    summary.members().stream()
                            .map(member -> ProjectMemberProfileResponse.from(
                                    member,
                                    mediaUrls,
                                    userAvatarUrls
                            ))
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

}
