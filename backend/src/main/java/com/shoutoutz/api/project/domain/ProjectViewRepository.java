package com.shoutoutz.api.project.domain;

import com.shoutoutz.api.visitor.domain.VisitorKey;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 프로젝트 조회 기록 저장소. 방문자·날짜별 조회를 project_view_days 에 남기고 조회수를 올린다.
 */
public interface ProjectViewRepository {

    /**
     * 조회를 기록할 수 있는 프로젝트인지 확인한다. 승인되고 삭제되지 않은 프로젝트만 해당한다.
     */
    boolean existsViewableProject(long projectId);

    /**
     * 같은 날 같은 방문자의 첫 조회일 때만 기록하고 조회수를 1 올린다.
     * 새로 기록했으면 true, 이미 기록돼 있었으면 false 다.
     */
    boolean record(long projectId, VisitorKey visitorKey, LocalDate viewedOn, Instant viewedAt);
}
