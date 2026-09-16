package com.shoutoutz.api.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Project save(Project project, List<Long> techTagIds, List<Long> memberIds);

    boolean existsBySlug(Slug slug);

    /**
     * 같은 리포지토리를 가리키는 프로젝트가 이미 있는지 확인한다.
     * GithubRepositoryUrl이 표기를 정규화해서 갖고 있으므로, 값 비교만으로 같은 리포지토리를 찾는다.
     */
    boolean existsByGithubRepositoryUrl(GithubRepositoryUrl githubRepositoryUrl);

    /**
     * 수정할 프로젝트 자신은 빼고, 같은 리포지토리를 가리키는 다른 프로젝트가 있는지 확인한다.
     */
    boolean existsByGithubRepositoryUrlExcluding(GithubRepositoryUrl githubRepositoryUrl, long projectId);

    /**
     * 삭제되지 않은 프로젝트를 한 건 조회한다. 승인 상태와 무관하게 찾는다.
     */
    Optional<Project> findActiveById(long projectId);

    /**
     * 프로젝트에 달린 기술 스택 id 를 저장된 순서대로 조회한다.
     */
    List<Long> findTechTagIds(long projectId);

    /**
     * 프로젝트의 팀원 id 를 저장된 순서대로 조회한다. 등록자가 0번이다.
     */
    List<Long> findMemberIds(long projectId);

    /**
     * 프로젝트 내용과 기술 스택, 팀원을 함께 수정한다.
     * 기술 스택과 팀원은 받은 목록으로 통째로 바꾸며, 목록 순서가 그대로 노출 순서가 된다.
     */
    Project update(Project project, List<Long> techTagIds, List<Long> memberIds);

    boolean existsPublicById(long projectId);
  
    /**
     * 삭제되지 않은 프로젝트의 상세 정보를 조회
     * @param viewerId 요청한 사용자 ID. 비로그인이면 null이며, 이때 likedByMe와 bookmarkedByMe는 false다.
     */
    Optional<ProjectDetail> findDetailById(long projectId, Long viewerId);

    /**
     * 승인되고 삭제되지 않은 프로젝트 목록을 조건에 맞게 한 페이지 조회한다.
     */
    ProjectPage findAll(ProjectSearchCondition condition);

    /**
     * 승인되고 삭제되지 않은 프로젝트 중 조건에 맞는 프로젝트 수를 필터 선택지별로 조회한다.
     */
    ProjectFilterOptions findFilterOptions(ProjectFilterCondition condition);
}
