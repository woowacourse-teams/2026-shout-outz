package com.shoutoutz.api.project.domain;

/**
 * 팀 블로그 원천 등록부(blog_sources) 저장소
 * 블로그 수집기가 읽는 등록부라, 프로젝트 저장소와 나눠 둔다.
 */
public interface BlogSourceRepository {

    /**
     * 팀이 폼에 입력한 블로그 주소(SERVICE_INPUT)를 받은 목록과 같게 맞춘다.
     * 목록에 있는 주소는 CONFIRMED 로 두고 목록 순서를 노출 순서로 쓴다.
     * 목록에서 빠진 주소는 수집 이력이 끊기지 않도록 지우지 않고 DISABLED 로 바꾼다.
     * 운영자가 넣은 주소(MANUAL)는 건드리지 않는다.
     */
    void syncServiceInputs(long projectId, TeamBlogUrls teamBlogUrls);
}
