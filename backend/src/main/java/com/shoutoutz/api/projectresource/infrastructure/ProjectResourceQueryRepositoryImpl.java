package com.shoutoutz.api.projectresource.infrastructure;

import static com.shoutoutz.api.projectresource.infrastructure.TeamResourceSql.TEAM_RESOURCE_CONDITION;
import static com.shoutoutz.api.projectresource.infrastructure.TeamResourceSql.addTeamSources;

import com.shoutoutz.api.projectresource.application.ProjectResourceQueryRepository;
import com.shoutoutz.api.projectresource.domain.CareerCard;
import com.shoutoutz.api.projectresource.domain.ProjectResourceJobRole;
import com.shoutoutz.api.projectresource.domain.ProjectResourceKind;
import com.shoutoutz.api.projectresource.domain.ResourceCount;
import com.shoutoutz.api.projectresource.domain.ResourceTopic;
import com.shoutoutz.api.projectresource.domain.TopicProject;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final String CARD_COLUMNS = """
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
            """;

    private static final String PUBLIC_PROJECT_JOIN = """
            LEFT JOIN projects project
                   ON project.id = document.project_id
                  AND project.approval_status = 'APPROVED'
                  AND project.deleted_at IS NULL
            """;

    private static final String CARD_SELECT = "SELECT\n" + CARD_COLUMNS
            + "FROM resource_documents document\n" + PUBLIC_PROJECT_JOIN;

    /**
     * 이 프로젝트의 경력 카드. 직군은 BE, FE, AN 순이고, 직군 안에서는 이력서에서 강한 순서(sort_order)다.
     */
    private static final String FIND_CAREER_CARDS_SQL = """
            SELECT card.id, card.track, card.template, card.category, card.title, card.resume_bullets, card.tech,
                   card.problem, card.cause, card.options, card.decision_reason, card.solution, card.result, card.limits
            FROM resource_career_cards card
            WHERE card.project_id = :projectId
              AND card.deleted_at IS NULL
              AND card.track IN ('BE', 'FE', 'AN')
            ORDER BY CASE card.track WHEN 'BE' THEN 0 WHEN 'FE' THEN 1 ELSE 2 END, card.sort_order, card.id
            """;

    /**
     * 카드 근거: 카드 출처 경험의 자료. 삭제된 경험과 자료, 받아들인 필요 없어요 제보 자료는 뺀다.
     */
    private static final String FIND_CAREER_CARD_SOURCES_SQL = "SELECT\n    card_source.card_id,\n" + CARD_COLUMNS
            + """
            FROM resource_career_card_sources card_source
            JOIN resource_experiences experience
              ON experience.id = card_source.experience_id
             AND experience.deleted_at IS NULL
            JOIN resource_documents document
              ON document.id = experience.document_id
            """ + PUBLIC_PROJECT_JOIN + """
            WHERE card_source.card_id IN (:cardIds)
              AND document.deleted_at IS NULL
            """ + TeamResourceSql.NOT_REMOVED_BY_REPORT + """
            ORDER BY card_source.card_id, card_source.source_order, document.id
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

    @Override
    public List<CareerCard> findCareerCards(long projectId) {
        return jdbcTemplate.query(
                FIND_CAREER_CARDS_SQL,
                new MapSqlParameterSource("projectId", projectId),
                (resultSet, rowNumber) -> new CareerCard(
                        resultSet.getLong("id"),
                        resultSet.getString("track"),
                        resultSet.getString("template"),
                        resultSet.getString("category"),
                        resultSet.getString("title"),
                        textArray(resultSet, "resume_bullets"),
                        textArray(resultSet, "tech"),
                        resultSet.getString("problem"),
                        resultSet.getString("cause"),
                        textArray(resultSet, "options"),
                        resultSet.getString("decision_reason"),
                        resultSet.getString("solution"),
                        resultSet.getString("result"),
                        resultSet.getString("limits")
                )
        );
    }

    @Override
    public Map<Long, List<ResourceCard>> findCareerCardSources(Collection<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ResourceCard>> sourcesByCard = new LinkedHashMap<>();
        jdbcTemplate.query(
                FIND_CAREER_CARD_SOURCES_SQL,
                new MapSqlParameterSource("cardIds", cardIds),
                resultSet -> {
                    sourcesByCard.computeIfAbsent(resultSet.getLong("card_id"), cardId -> new ArrayList<>())
                            .add(toCard(resultSet));
                }
        );
        return sourcesByCard;
    }

    private static List<String> textArray(ResultSet resultSet, String column) throws SQLException {
        Array array = resultSet.getArray(column);
        if (array == null) {
            return List.of();
        }
        try {
            return List.of((String[]) array.getArray());
        } finally {
            array.free();
        }
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
