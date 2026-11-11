package com.shoutoutz.api.project.domain;

/**
 * 승인되고 삭제되지 않은 프로젝트의 이름. 검색어가 프로젝트 이름과 같은지 비교할 때 쓴다.
 */
public record PublicProjectTitle(long id, String title) {
}
