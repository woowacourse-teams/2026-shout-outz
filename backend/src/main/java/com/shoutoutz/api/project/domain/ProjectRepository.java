package com.shoutoutz.api.project.domain;

import java.time.Instant;
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
     * 등록자 본인의 삭제되지 않은 프로젝트를 소프트 삭제하고, 삭제 이력에 남길 삭제 시점 정보를 돌려준다.
     * 삭제는 심사 중이어도 가능하므로, 승인 상태는 보지 않는다.
     * 없는 프로젝트, 남의 프로젝트, 이미 삭제된 프로젝트는 모두 빈 값이다.
     */
    Optional<DeletedProject> softDelete(long projectId, long registeredBy, Instant deletedAt);

    /**
     * 등록자 본인의 삭제된 프로젝트와 아직 복구되지 않은 삭제 이력을 함께 조회한다.
     * 없는 프로젝트, 남의 프로젝트, 삭제되지 않은 프로젝트는 모두 빈 값이다.
     */
    Optional<RestorableProject> findRestorable(long projectId, long registeredBy);

    /**
     * 삭제된 프로젝트를 복구하고 실제로 복구한 행 수를 돌려준다. 이미 복구된 프로젝트는 0 이다.
     */
    int restore(long projectId, Instant restoredAt);
}
