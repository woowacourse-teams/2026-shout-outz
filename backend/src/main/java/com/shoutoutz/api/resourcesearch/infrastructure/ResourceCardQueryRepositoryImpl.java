package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceCardQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTeam;
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
            d.extra ->> 'project_name' AS collected_team_name,
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
        List<ResourceCard> cards = jdbcTemplate.query(
                "SELECT " + CARD_COLUMNS + " FROM resource_documents d " + VISIBLE_PROJECT_JOIN + """
                        WHERE d.id IN (:resourceIds)
                          AND d.deleted_at IS NULL
                        """,
                new MapSqlParameterSource("resourceIds", resourceIds),
                (resultSet, rowNumber) -> mapCard(resultSet)
        );
        return inRequestedOrder(resourceIds, cards);
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
                ResourceTeam.resolve(
                        resultSet.getString("team_name"),
                        resultSet.getString("slug"),
                        resultSet.getObject("thumbnail_media_id", Long.class),
                        resultSet.getString("collected_team_name")
                )
        );
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
