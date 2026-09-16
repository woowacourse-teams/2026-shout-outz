package com.shoutoutz.api.project.application;

import com.shoutoutz.api.project.application.dto.UserProjectResult;
import com.shoutoutz.api.project.domain.ProjectCursor;

/**
 * 사용자가 참여한 프로젝트 목록 조회 포트.
 */
public interface UserProjectQueryRepository {

    UserProjectResult findAllByUserId(long userId, ProjectCursor cursor, int size);
}
