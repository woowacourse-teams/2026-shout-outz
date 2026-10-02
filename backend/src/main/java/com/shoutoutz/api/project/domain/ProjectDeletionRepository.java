package com.shoutoutz.api.project.domain;

import java.time.Instant;

/**
 * 프로젝트 삭제 이력 저장소. 삭제와 복구가 일어난 사건을 기록한다.
 * 프로젝트의 현재 삭제 상태는 ProjectRepository 가 다룬다.
 */
public interface ProjectDeletionRepository {

    /**
     * 삭제할 때마다 새 이력을 남긴다. 기존 이력은 고치지 않으므로 id 가 없는 이력만 넘긴다.
     */
    ProjectDeletion save(ProjectDeletion deletion);

    /**
     * 미복구 삭제 이력에 복구 주체와 복구 시각을 남기고 실제로 수정한 행 수를 돌려준다.
     * 이미 복구된 이력은 0 이다.
     */
    int markRestored(long deletionId, long restoredBy, Instant restoredAt);
}
