package com.shoutoutz.api.project.domain;

/**
 * 소프트 삭제된 프로젝트의 삭제 시점 정보. 삭제 이력에 그대로 복사해 둔다.
 */
public record DeletedProject(long id, String slug, String title) {
}
