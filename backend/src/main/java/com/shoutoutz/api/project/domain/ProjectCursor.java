package com.shoutoutz.api.project.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * 프로젝트 목록에서 마지막으로 조회한 프로젝트의 위치
 * 정렬 기준에 쓰인 값을 그대로 담아, 다음 페이지는 이 위치 다음부터 조회한다.
 * 좋아요 수는 인기순(POPULAR)일 때만 있다.
 *
 * 직접 검색은 순위를 매 요청 다시 계산하면 그사이 순위가 바뀌거나 프로젝트가 삭제될 때 겹치거나 빠질 수 있다.
 * 그래서 첫 페이지에서 정한 순위 목록(rankedIds)과 읽은 위치(position)를 커서에 담아 다음 페이지가 그 목록을 이어 읽는다.
 * 이때 등록 시각과 프로젝트 ID는 쓰지 않는다.
 *
 * @param rankedIds 직접 검색의 순위 목록. 순위 목록 커서가 아니면 null
 * @param position  순위 목록에서 마지막으로 내려준 프로젝트의 위치(1부터). 순위 목록 커서가 아니면 0
 */
public record ProjectCursor(
        ProjectSort sort,
        Long likeCount,
        Instant createdAt,
        long id,
        List<Long> rankedIds,
        int position
) {

    /**
     * 클라이언트가 커서를 바꿔 보내도 조회 크기가 커지지 않도록, 직접 검색의 프로젝트 상한과 같은 수까지만 받는다.
     */
    public static final int MAX_RANKED_IDS = 30;

    public ProjectCursor {
        Objects.requireNonNull(sort, "커서의 정렬 기준은 null일 수 없습니다.");
        if (rankedIds != null) {
            rankedIds = List.copyOf(rankedIds);
            validateRanked(sort, likeCount, createdAt, rankedIds, position);
        } else {
            validatePositional(sort, likeCount, createdAt, id, position);
        }
    }

    public ProjectCursor(ProjectSort sort, Long likeCount, Instant createdAt, long id) {
        this(sort, likeCount, createdAt, id, null, 0);
    }

    public static ProjectCursor latest(Instant createdAt, long id) {
        return new ProjectCursor(ProjectSort.LATEST, null, createdAt, id);
    }

    /**
     * 순위 목록 없이 관련도순으로 조회할 때(주제 목록 등)는 최신순처럼 등록 시각을 쓴다.
     */
    public static ProjectCursor relevance(Instant createdAt, long id) {
        return new ProjectCursor(ProjectSort.RELEVANCE, null, createdAt, id);
    }

    public static ProjectCursor popular(long likeCount, Instant createdAt, long id) {
        return new ProjectCursor(ProjectSort.POPULAR, likeCount, createdAt, id);
    }

    public static ProjectCursor ranked(List<Long> rankedIds, int position) {
        return new ProjectCursor(ProjectSort.RELEVANCE, null, null, 0, rankedIds, position);
    }

    public boolean isRanked() {
        return rankedIds != null;
    }

    private static void validatePositional(ProjectSort sort, Long likeCount, Instant createdAt, long id, int position) {
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
        if (position != 0) {
            throw new IllegalArgumentException("순위 목록 위치는 순위 목록 커서에만 있어야 합니다.");
        }
    }

    /**
     * 다음 페이지가 있을 때만 커서를 만들므로, 위치는 목록의 마지막보다 앞이어야 한다.
     */
    private static void validateRanked(
            ProjectSort sort,
            Long likeCount,
            Instant createdAt,
            List<Long> rankedIds,
            int position
    ) {
        if (sort != ProjectSort.RELEVANCE || likeCount != null || createdAt != null) {
            throw new IllegalArgumentException("순위 목록 커서는 관련도순에서 순위 목록과 위치만 담는다.");
        }
        if (rankedIds.isEmpty() || rankedIds.size() > MAX_RANKED_IDS) {
            throw new IllegalArgumentException("순위 목록은 1개 이상 " + MAX_RANKED_IDS + "개 이하여야 합니다.");
        }
        if (rankedIds.stream().anyMatch(rankedId -> rankedId == null || rankedId <= 0)) {
            throw new IllegalArgumentException("순위 목록의 프로젝트 ID는 0보다 커야 합니다.");
        }
        if (new HashSet<>(rankedIds).size() != rankedIds.size()) {
            throw new IllegalArgumentException("순위 목록에 같은 프로젝트 ID가 두 번 있을 수 없습니다.");
        }
        if (position < 1 || position >= rankedIds.size()) {
            throw new IllegalArgumentException("순위 목록 위치는 1 이상, 목록 길이보다 작아야 합니다.");
        }
    }
}
