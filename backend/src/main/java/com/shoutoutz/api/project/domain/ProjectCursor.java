package com.shoutoutz.api.project.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 프로젝트 목록에서 마지막으로 조회한 프로젝트의 위치
 * 정렬 기준에 쓰인 값을 그대로 담아, 다음 페이지는 이 위치 다음부터 조회한다.
 * 좋아요 수는 인기순(POPULAR)일 때만 있다.
 */
public record ProjectCursor(
        ProjectSort sort,
        Long likeCount,
        Instant createdAt,
        long id
) {

    public ProjectCursor {
        Objects.requireNonNull(sort, "커서의 정렬 기준은 null일 수 없습니다.");
        Objects.requireNonNull(createdAt, "커서의 등록 시각은 null일 수 없습니다.");
        if (id <= 0) {
            throw new IllegalArgumentException("커서의 프로젝트 ID는 0보다 커야 합니다.");
        }
        if ((sort == ProjectSort.POPULAR) == (likeCount == null)) {
            throw new IllegalArgumentException("좋아요 수는 인기순 커서에만 있어야 합니다.");
        }
        if (likeCount != null && likeCount < 0) {
            throw new IllegalArgumentException("커서의 좋아요 수는 0 이상이어야 합니다.");
        }
    }

    public static ProjectCursor latest(Instant createdAt, long id) {
        return new ProjectCursor(ProjectSort.LATEST, null, createdAt, id);
    }

    public static ProjectCursor popular(long likeCount, Instant createdAt, long id) {
        return new ProjectCursor(ProjectSort.POPULAR, likeCount, createdAt, id);
    }
}
