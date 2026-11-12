package com.shoutoutz.api.project.domain;

/**
 * 직접 검색에서 필터를 통과한 후보 프로젝트
 *
 * @param nameMatched 프로젝트 이름이나 저장소 주소에 검색어가 들어 있는지
 */
public record KeywordCandidate(long id, boolean nameMatched) {
}
