package com.shoutoutz.api.resourcesearch.infrastructure;

import static com.shoutoutz.api.resourcesearch.infrastructure.ResourceCardQueryRepositoryImpl.CARD_COLUMNS;
import static com.shoutoutz.api.resourcesearch.infrastructure.ResourceCardQueryRepositoryImpl.VISIBLE_PROJECT_JOIN;

import com.shoutoutz.api.resourcesearch.application.ResourceDetailQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceDetail;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceExperience;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceDetailQueryRepositoryImpl implements ResourceDetailQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<ResourceDetail> findById(long resourceId) {
        String sql = """
                SELECT %s, d.content_type, d.key_points, t.id AS topic_id, t.name AS topic_name
                FROM resource_documents d
                %s
                LEFT JOIN resource_topic_catalog t ON t.id = d.topic
                WHERE d.id = :resourceId
                  AND d.deleted_at IS NULL
                """.formatted(CARD_COLUMNS, VISIBLE_PROJECT_JOIN);
        MapSqlParameterSource parameters = new MapSqlParameterSource("resourceId", resourceId);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new ResourceDetail(
                        ResourceCardQueryRepositoryImpl.mapCard(resultSet),
                        resultSet.getString("content_type"),
                        topicOf(resultSet),
                        textsOf(resultSet.getArray("key_points")),
                        findExperiences(resourceId)
                ))
                .stream()
                .findFirst();
    }

    private List<ResourceExperience> findExperiences(long resourceId) {
        return jdbcTemplate.query(
                """
                        SELECT problem, cause, solution, result, tech
                        FROM resource_experiences
                        WHERE document_id = :resourceId
                          AND deleted_at IS NULL
                        ORDER BY experience_index
                        """,
                new MapSqlParameterSource("resourceId", resourceId),
                (resultSet, rowNumber) -> new ResourceExperience(
                        resultSet.getString("problem"),
                        resultSet.getString("cause"),
                        resultSet.getString("solution"),
                        resultSet.getString("result"),
                        textsOf(resultSet.getArray("tech"))
                )
        );
    }

    private ResourceTopic topicOf(ResultSet resultSet) throws SQLException {
        String topicId = resultSet.getString("topic_id");
        if (topicId == null) {
            return null;
        }
        return new ResourceTopic(topicId, resultSet.getString("topic_name"));
    }

    private List<String> textsOf(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.asList((String[]) array.getArray());
    }
}
