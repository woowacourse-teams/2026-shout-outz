package com.shoutoutz.api.project.domain;

import java.util.List;

public interface ProjectRepository {

    Project save(Project project, List<Long> techTagIds, List<Long> memberIds);

    boolean existsBySlug(Slug slug);

    /**
     * 승인되고 삭제되지 않은 프로젝트 목록을 조건에 맞게 한 페이지 조회한다.
     */
    ProjectPage findAll(ProjectSearchCondition condition);
}
