package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceCardQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.domain.ResourceSection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceCardQueryRepositoryImpl implements ResourceCardQueryRepository {

    static final String CARD_COLUMNS = """
            d.id, d.source, d.title, d.url, d.summary, d.published_at,
            d.extra ->> 'platform' AS platform,
            CASE WHEN d.project_id IS NULL THEN d.extra ->> 'project_name' END AS collected_team_name,
            p.team_name, p.slug, p.thumbnail_media_id
            """;

    static final String VISIBLE_PROJECT_JOIN = """
            LEFT JOIN projects p
                ON p.id = d.project_id
               AND p.approval_status = 'APPROVED'
               AND p.deleted_at IS NULL
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ResourceCard> findByIds(List<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT %s
                FROM resource_documents d
                %s
                WHERE d.id IN (:resourceIds)
                  AND d.deleted_at IS NULL
                """.formatted(CARD_COLUMNS, VISIBLE_PROJECT_JOIN);
        List<ResourceCard> cards = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("resourceIds", resourceIds),
                (resultSet, rowNumber) -> mapCard(resultSet)
        );
        return inRequestedOrder(resourceIds, cards);
    }

    @Override
    public List<ResourceCard> findRecent(ResourceSection kind, int size) {
        String sql = """
                SELECT %s
                FROM (
                    SELECT d.*,
                           ROW_NUMBER() OVER (
                               PARTITION BY %s
                               ORDER BY d.published_at DESC, d.id DESC
                           ) AS author_rank
                    FROM resource_documents d
                    WHERE d.source IN (:sources)
                      AND d.deleted_at IS NULL
                      AND d.published_at IS NOT NULL
                      AND NULLIF(btrim(d.summary), '') IS NOT NULL
                ) d
                %s
                WHERE d.author_rank = 1
                ORDER BY d.published_at DESC, d.id DESC
                LIMIT :size
                """.formatted(CARD_COLUMNS, onePerAuthorKey(kind), VISIBLE_PROJECT_JOIN);
        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("sources", kind.sources())
                        .addValue("size", size),
                (resultSet, rowNumber) -> mapCard(resultSet)
        );
    }

    private String onePerAuthorKey(ResourceSection kind) {
        return switch (kind) {
            case BLOG -> "COALESCE('project:' || d.project_id, 'team:' || NULLIF(btrim(d.extra ->> 'project_name'), ''), 'resource:' || d.id)";
            case TALK -> "COALESCE('speaker:' || NULLIF(btrim(d.extra ->> 'speaker'), ''), 'resource:' || d.id)";
            case DOCUMENT -> throw new IllegalArgumentException("최근 자료는 블로그와 테코톡만 조회할 수 있습니다.");
        };
    }

    static ResourceCard mapCard(ResultSet resultSet) throws SQLException {
        return new ResourceCard(
                resultSet.getLong("id"),
                resultSet.getString("source"),
                resultSet.getString("title"),
                resultSet.getString("url"),
                resultSet.getString("summary"),
                toInstant(resultSet.getTimestamp("published_at")),
                resultSet.getString("platform"),
                teamOf(resultSet)
        );
    }

    private static ResourceCard.Team teamOf(ResultSet resultSet) throws SQLException {
        String projectSlug = resultSet.getString("slug");
        if (projectSlug != null) {
            return new ResourceCard.Team(
                    resultSet.getString("team_name"),
                    projectSlug,
                    resultSet.getObject("thumbnail_media_id", Long.class)
            );
        }
        String collectedTeamName = resultSet.getString("collected_team_name");
        if (collectedTeamName == null || collectedTeamName.isBlank()) {
            return null;
        }
        return new ResourceCard.Team(collectedTeamName, null, null);
    }

    private static Instant toInstant(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant();
    }

    private List<ResourceCard> inRequestedOrder(List<Long> resourceIds, List<ResourceCard> cards) {
        Map<Long, ResourceCard> cardsById = cards.stream()
                .collect(Collectors.toMap(ResourceCard::resourceId, Function.identity()));
        return resourceIds.stream()
                .map(cardsById::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
