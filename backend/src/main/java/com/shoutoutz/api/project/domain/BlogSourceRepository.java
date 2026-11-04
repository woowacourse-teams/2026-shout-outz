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

    /**
     * 프로젝트 없이 레포 주소로만 넣어 둔 블로그(MANUAL)를 이 프로젝트에 이어 붙인다.
     * 서비스에 등록하기 전부터 수집하던 팀 블로그가 등록 뒤에도 같은 행으로 이어지게 한다.
     * 프로젝트에 이미 같은 블로그 주소가 있으면 그 행이 블로그를 맡으므로, 레포 주소로 넣어 둔 행은 DISABLED 로 바꾼다.
     * 팀이 폼에 같은 블로그 주소를 넣어도 운영자 행이 남도록, syncServiceInputs 보다 먼저 부른다.
     */
    void attachUnregistered(long projectId, GithubRepositoryUrl githubRepositoryUrl);
}
