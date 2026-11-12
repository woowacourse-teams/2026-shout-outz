package com.shoutoutz.api.project.domain;

import java.time.Instant;

/**
 * 직접 검색에서 필터를 통과한 후보 프로젝트
 *
 * @param nameMatched 프로젝트 이름이나 저장소 이름에 검색어가 들어 있는지
 * @param createdAt   등록 시각. 이관 프로젝트는 원래 등록 시각이라 id 순서와 다를 수 있다
 */
public record KeywordCandidate(long id, boolean nameMatched, Instant createdAt) {
}
