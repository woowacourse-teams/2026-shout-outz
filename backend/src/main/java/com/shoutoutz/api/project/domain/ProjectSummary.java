package com.shoutoutz.api.project.domain;

import java.time.Instant;

/**
 * 프로젝트 목록 카드에 필요한 조회 모델
 * 등록자(registeredBy)가 없으면, 이전 기수에서 이관된 프로젝트다.
 */
public record ProjectSummary(
        long id,
        String slug,
        String title,
        String tagline,
        int cohort,
        Long thumbnailMediaId,
        Long registeredBy,
        long likeCount,
        long commentCount,
        Instant createdAt
) {

    /**
     * 이 프로젝트를 기준으로 다음 페이지를 조회하는 커서
     * 정렬에 쓰인 값을 그대로 담아야 다음 페이지에서 이 프로젝트 바로 다음부터 조회할 수 있다.
     */
    public ProjectCursor toCursor(ProjectSort sort) {
        if (sort == ProjectSort.POPULAR) {
            return ProjectCursor.popular(likeCount, createdAt, id);
        }
        return ProjectCursor.latest(createdAt, id);
    }
}
