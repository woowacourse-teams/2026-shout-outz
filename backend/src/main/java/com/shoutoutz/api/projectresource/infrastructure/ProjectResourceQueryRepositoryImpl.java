package com.shoutoutz.api.projectresource.infrastructure;

import static com.shoutoutz.api.projectresource.infrastructure.TeamResourceSql.TEAM_RESOURCE_CONDITION;
import static com.shoutoutz.api.projectresource.infrastructure.TeamResourceSql.addTeamSources;

import com.shoutoutz.api.projectresource.application.ProjectResourceQueryRepository;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ProjectResourceKind;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.domain.TopicProject;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectResourceQueryRepositoryImpl implements ProjectResourceQueryRepository {

    private static final String FIND_TOPICS_SQL = """
            SELECT id, name, side, category
            FROM resource_topic_catalog
            """;

    /**
     * 자료 직군이 고른 직군이거나 ALL, 없음인 자료만 남긴다.
     */
    private static final String TRACK_CONDITION = """
              AND (document.track IS NULL OR document.track = 'ALL' OR document.track = :track)
            """;

    private static final String COUNT_TEAM_RESOURCES_SQL = """
            SELECT document.topic, document.track, document.source, COUNT(*) AS resource_count
            FROM resource_documents document
            WHERE document.project_id = :projectId
              AND document.topic IS NOT NULL
            """ + TEAM_RESOURCE_CONDITION + """
            GROUP BY document.topic, document.track, document.source
            """;

    /**
     * 공개된 프로젝트의 팀 자료가 있는 (주제, 프로젝트). 탐색 화면 기술 주제 목록이 주제별 프로젝트 수를 셀 때 쓴다.
     */
    private static final String FIND_TOPIC_PROJECTS_SQL = """
            SELECT DISTINCT document.topic, document.project_id
            FROM resource_documents document
            JOIN projects project
              ON project.id = document.project_id
             AND project.approval_status = 'APPROVED'
             AND project.deleted_at IS NULL
            WHERE document.topic IS NOT NULL
            """ + TEAM_RESOURCE_CONDITION;

    /**
     * 자료 카드 칸. 팀은 공개된 프로젝트면 그 프로젝트로, 프로젝트에 이어지지 않은 자료면 수집 때 저장한 팀 이름으로 채운다.
     */
    private static final String CARD_SELECT = """
            SELECT
                document.id,
                document.source,
                document.title,
                document.url,
                document.summary,
                document.published_at,
                CASE WHEN document.source = 'blog-post' THEN document.extra ->> 'platform' END AS platform,
                project.team_name,
                project.slug AS project_slug,
                project.thumbnail_media_id,
                CASE WHEN document.project_id IS NULL THEN document.extra ->> 'project_name' END AS unregistered_team_name
            FROM resource_documents document
            LEFT JOIN projects project
                   ON project.id = document.project_id
                  AND project.approval_status = 'APPROVED'
                  AND project.deleted_at IS NULL
            """;

    private static final String FIND_TEAM_RESOURCES_SQL = CARD_SELECT + """
            WHERE document.project_id = :projectId
              AND document.topic = :topicId
            """ + TEAM_RESOURCE_CONDITION;

    /**
     * 프로토타입처럼 블로그를 먼저 보이고, 그다음 점수 높은 순으로 줄 세운다.
     */
    private static final String TEAM_RESOURCE_ORDER = """
            ORDER BY document.source = 'blog-post' DESC,
                     document.score DESC NULLS LAST,
                     document.published_at DESC NULLS LAST,
                     document.id DESC
            """;

    private static final String FIND_TALKS_SQL = CARD_SELECT + """
            WHERE document.topic = :topicId
              AND document.deleted_at IS NULL
              AND document.source = :talkSource
            ORDER BY document.published_at DESC NULLS LAST, document.id DESC
            """;

    /**
     * 다른 팀 자료는 공개된 다른 프로젝트의 자료와 프로젝트에 이어지지 않은 자료(등록 전 팀의 블로그 등)다.
     * 공개되지 않은 프로젝트의 자료는 그 프로젝트를 없는 것으로 보고 뺀다.
     */
    private static final String FIND_OTHER_TEAM_RESOURCES_SQL = CARD_SELECT + """
            WHERE document.topic = :topicId
              AND (document.project_id IS NULL
                   OR (document.project_id <> :projectId AND project.id IS NOT NULL))
            """ + TEAM_RESOURCE_CONDITION;

    /**
     * 점수 높은 순, 같으면 최신순. 점수가 없는 자료(블로그)는 프로토타입처럼 85점으로 본다.
     */
    private static final String OTHER_TEAM_RESOURCE_ORDER = """
            ORDER BY COALESCE(document.score, 85) DESC,
                     document.published_at DESC NULLS LAST,
                     document.id DESC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ResourceTopic> findTopics() {
        return jdbcTemplate.query(FIND_TOPICS_SQL, (resultSet, rowNumber) -> toTopic(resultSet));
    }

    @Override
    public Optional<ResourceTopic> findTopic(String topicId) {
        return jdbcTemplate.query(
                FIND_TOPICS_SQL + "WHERE id = :topicId",
                new MapSqlParameterSource("topicId", topicId),
                (resultSet, rowNumber) -> toTopic(resultSet)
        ).stream().findFirst();
    }

    @Override
    public List<ResourceCount> countTeamResources(long projectId) {
        return jdbcTemplate.query(
                COUNT_TEAM_RESOURCES_SQL,
                addTeamSources(new MapSqlParameterSource("projectId", projectId)),
                (resultSet, rowNumber) -> new ResourceCount(
                        resultSet.getString("topic"),
                        resultSet.getString("track"),
                        resultSet.getString("source"),
                        resultSet.getLong("resource_count")
                )
        );
    }

    @Override
    public List<TopicProject> findTopicProjects() {
        return jdbcTemplate.query(
                FIND_TOPIC_PROJECTS_SQL,
                addTeamSources(new MapSqlParameterSource()),
                (resultSet, rowNumber) -> new TopicProject(
                        resultSet.getString("topic"),
                        resultSet.getLong("project_id")
                )
        );
    }

    @Override
    public List<ResourceCard> findTeamResources(long projectId, String topicId, ProjectResourceJobRole jobRole) {
        MapSqlParameterSource parameters = addTeamSources(new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("topicId", topicId));
        String sql = FIND_TEAM_RESOURCES_SQL + trackCondition(jobRole, parameters) + TEAM_RESOURCE_ORDER;
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> toCard(resultSet));
    }

    @Override
    public List<ResourceCard> findTalks(String topicId) {
        return jdbcTemplate.query(
                FIND_TALKS_SQL,
                new MapSqlParameterSource()
                        .addValue("topicId", topicId)
                        .addValue("talkSource", ProjectResourceKind.TALK_SOURCE),
                (resultSet, rowNumber) -> toCard(resultSet)
        );
    }

    @Override
    public List<ResourceCard> findOtherTeamResources(
            long projectId,
            String topicId,
            ProjectResourceJobRole jobRole,
            int limit
    ) {
        MapSqlParameterSource parameters = addTeamSources(new MapSqlParameterSource()
                .addValue("projectId", projectId)
                .addValue("topicId", topicId)
                .addValue("limit", limit));
        String sql = FIND_OTHER_TEAM_RESOURCES_SQL + trackCondition(jobRole, parameters) + OTHER_TEAM_RESOURCE_ORDER;
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> toCard(resultSet));
    }

    private static String trackCondition(ProjectResourceJobRole jobRole, MapSqlParameterSource parameters) {
        if (jobRole == ProjectResourceJobRole.ALL) {
            return "";
        }
        parameters.addValue("track", jobRole.name());
        return TRACK_CONDITION;
    }

    private static ResourceTopic toTopic(ResultSet resultSet) throws SQLException {
        return new ResourceTopic(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("side"),
                resultSet.getString("category")
        );
    }

    private static ResourceCard toCard(ResultSet resultSet) throws SQLException {
        OffsetDateTime publishedAt = resultSet.getObject("published_at", OffsetDateTime.class);
        return new ResourceCard(
                resultSet.getLong("id"),
                resultSet.getString("source"),
                resultSet.getString("title"),
                resultSet.getString("url"),
                resultSet.getString("summary"),
                publishedAt == null ? null : publishedAt.toInstant(),
                resultSet.getString("platform"),
                toTeam(resultSet)
        );
    }

    private static ResourceCard.Team toTeam(ResultSet resultSet) throws SQLException {
        String projectSlug = resultSet.getString("project_slug");
        if (projectSlug != null) {
            return new ResourceCard.Team(
                    resultSet.getString("team_name"),
                    projectSlug,
                    resultSet.getObject("thumbnail_media_id", Long.class)
            );
        }
        String unregisteredTeamName = resultSet.getString("unregistered_team_name");
        return unregisteredTeamName == null ? null : new ResourceCard.Team(unregisteredTeamName, null, null);
    }
}
