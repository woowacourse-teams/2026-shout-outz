package com.shoutoutz.api.projectresource.application;

import com.shoutoutz.api.project.domain.GithubRepositoryUrl;

/**
 * 수집한 기술 자료와 전체 경험 카드를 프로젝트에 잇는다.
 * 데이터 파이프라인은 프로젝트를 모르고 레포 주소와 글 주소만 남긴다. 그래서 팀이 프로젝트를 등록하기 전에 모은 자료는
 * 프로젝트 없이 들어가 있다가, 프로젝트를 등록하거나 고칠 때 여기서 잇는다.
 * 살아 있는 다른 프로젝트에 이어진 자료는 건드리지 않는다. 삭제된 프로젝트에 남은 자료는 새로 등록한 프로젝트가 가져온다.
 */
public interface ProjectResourceLinkRepository {

    /**
     * 레포 주소가 같은 PR, 팀 문서, 위키, 디스커션과 레포가 같은 전체 경험 카드를 이 프로젝트에 잇는다.
     * 이 프로젝트의 확정된 팀 블로그 주소로 시작하는 블로그 글도 잇는다. 다른 프로젝트의 확정된 블로그 주소에도
     * 걸리는 글은 어느 팀 글인지 알 수 없어 잇지 않는다.
     * 이은 자료의 경험과 검색 조각도 같은 프로젝트로 맞춘다. 여러 번 불러도 결과가 같다.
     */
    void link(long projectId, GithubRepositoryUrl githubRepositoryUrl);

    /**
     * 이 프로젝트에 이어진 자료 중 레포 주소가 같은 PR, 팀 문서, 위키, 디스커션과 전체 경험 카드를 프로젝트에서 푼다.
     * 레포 주소를 바꿀 때 예전 레포의 자료를 내려놓으려고 쓴다. 블로그 글은 레포가 아니라 블로그 주소로 이었으므로 그대로 둔다.
     */
    void unlink(long projectId, GithubRepositoryUrl githubRepositoryUrl);
}
