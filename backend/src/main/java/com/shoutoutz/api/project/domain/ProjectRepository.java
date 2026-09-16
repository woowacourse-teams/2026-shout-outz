package com.shoutoutz.api.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Project save(Project project, List<Long> techTagIds, List<Long> memberIds);

    boolean existsBySlug(Slug slug);

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
