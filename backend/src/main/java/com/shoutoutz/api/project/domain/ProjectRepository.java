package com.shoutoutz.api.project.domain;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {

    Project save(Project project, List<Long> techTagIds, List<Long> memberIds);

    boolean existsBySlug(Slug slug);

    /**
     * 삭제되지 않은 프로젝트의 상세 정보를 조회
     * @param viewerId 요청한 사용자 ID. 비로그인이면 null이며, 이때 likedByMe와 bookmarkedByMe는 false다.
     */
    Optional<ProjectDetail> findDetailById(long projectId, Long viewerId);
}
