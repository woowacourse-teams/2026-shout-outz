package com.shoutoutz.api.resourcesearch.application.dto;

import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import java.util.Arrays;
import java.util.List;

/**
 * 자료 검색의 한 섹션을 이어 읽는 위치. 검색 경로에 따라 둘 중 하나만 담는다.
 *
 * @param project     프로젝트 이름 검색. 최신순 목록에서 앞 페이지 마지막 자료의 위치
 * @param rankedIds   연관도 검색. 첫 검색에서 정한 순위 중 아직 주지 않은 자료 ID. 이어 읽을 때 다시 검색하지 않아 순위가 바뀌지 않는다
 * @param rankedTotal 연관도 검색. 그 섹션에서 이어 읽기까지 내보내는 자료 전체 수
 */
public record ResourceSearchCursor(
        ProjectResourceCursor project,
        List<Long> rankedIds,
        int rankedTotal
) {

    private static final int MAX_RANKED = Arrays.stream(ResourceSection.values())
            .mapToInt(ResourceSection::depth)
            .max()
            .orElseThrow();

    public ResourceSearchCursor {
        if ((project == null) == (rankedIds == null)) {
            throw new IllegalArgumentException("커서는 프로젝트 자료 위치와 순위 목록 중 하나만 담아야 합니다.");
        }
        if (project != null && project.resourceId() <= 0) {
            throw new IllegalArgumentException("프로젝트 자료 위치의 자료 ID는 양수여야 합니다.");
        }
        if (rankedIds != null) {
            rankedIds = validRankedIds(rankedIds);
            if (rankedTotal < rankedIds.size() || rankedTotal > MAX_RANKED) {
                throw new IllegalArgumentException("순위 목록의 전체 수가 올바르지 않습니다.");
            }
        }
    }

    /**
     * 이어 읽을 자료가 없으면 커서도 없다.
     */
    public static ResourceSearchCursor ofProject(ProjectResourceCursor project) {
        if (project == null) {
            return null;
        }
        return new ResourceSearchCursor(project, null, 0);
    }

    public static ResourceSearchCursor ofRanked(List<Long> rankedIds, int rankedTotal) {
        return new ResourceSearchCursor(null, rankedIds, rankedTotal);
    }

    private static List<Long> validRankedIds(List<Long> rankedIds) {
        if (rankedIds.isEmpty() || rankedIds.size() > MAX_RANKED) {
            throw new IllegalArgumentException("순위 목록은 1건 이상, 깊이 상한 이하여야 합니다.");
        }
        for (Long id : rankedIds) {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException("순위 목록의 자료 ID는 양수여야 합니다.");
            }
        }
        return List.copyOf(rankedIds);
    }

    public boolean isRanked() {
        return rankedIds != null;
    }
}
