package com.shoutoutz.api.projectresource.infrastructure;

import com.shoutoutz.api.projectresource.domain.ProjectResourceKind;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * 팀 자료 조건. 프로젝트 상세 기술 이야기 탭, 탐색 화면 기술 주제 목록, 프로젝트 목록의 주제 조건이 이 규칙으로 자료를 센다.
 * 같은 규칙을 써야 주제 칩으로 고른 프로젝트의 기술 이야기 탭에 그 주제가 보인다.
 * 팀 자료는 삭제되지 않은 블로그, PR, 팀 문서이며, 관리자가 받아들인 필요 없어요 제보가 있는 자료는 뺀다.
 * SQL 은 자료 테이블 별칭을 document 로 쓰고, 원천 목록은 addTeamSources 로 넘긴다.
 */
public final class TeamResourceSql {

    /**
     * 관리자가 받아들인 필요 없어요 제보가 있는 자료를 뺀다. 상태를 되돌리면 다시 보인다.
     */
    public static final String NOT_REMOVED_BY_REPORT = """
              AND NOT EXISTS (
                  SELECT 1
                  FROM resource_reports report
                  WHERE report.kind = 'REMOVE'
                    AND report.status = 'ACCEPTED'
                    AND report.resource_source = document.source
                    AND report.resource_external_key = document.external_key
              )
            """;

    public static final String TEAM_RESOURCE_CONDITION = """
              AND document.deleted_at IS NULL
              AND document.source IN (:teamSources)
            """ + NOT_REMOVED_BY_REPORT;

    /**
     * 프로젝트 목록(별칭 p)에서 그 주제(:topicId)의 팀 자료가 하나라도 있는 프로젝트만 남긴다.
     */
    public static final String HAS_TOPIC_CONDITION = """
              AND EXISTS (
                  SELECT 1
                  FROM resource_documents document
                  WHERE document.project_id = p.id
                    AND document.topic = :topicId
            """ + TEAM_RESOURCE_CONDITION + """
              )
            """;

    private TeamResourceSql() {
    }

    public static MapSqlParameterSource addTeamSources(MapSqlParameterSource parameters) {
        return parameters.addValue("teamSources", ProjectResourceKind.teamSources());
    }
}
