package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceTopicQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.CatalogTopic;
import com.shoutoutz.api.resourcesearch.application.dto.PopularTopic;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceTopic;
import com.shoutoutz.api.resourcesearch.domain.ResourceTopicSide;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ResourceTopicQueryRepositoryImpl implements ResourceTopicQueryRepository {

    private static final String NAME_ORDER = "lower(t.name) COLLATE \"C\", t.name COLLATE \"C\", t.id";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<CatalogTopic> findAllOrderByName() {
        String sql = """
                SELECT t.id, t.name, t.side
                FROM resource_topic_catalog t
                ORDER BY %s
                """.formatted(NAME_ORDER);
        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new CatalogTopic(
                ResourceTopicSide.fromCode(resultSet.getString("side")),
                topicOf(resultSet)
        ));
    }

    @Override
    public List<PopularTopic> findMostCovered(int size) {
        String sql = """
                SELECT t.id, t.name, count(*) AS resource_count
                FROM resource_topic_catalog t
                JOIN resource_documents d ON d.topic = t.id AND d.deleted_at IS NULL
                GROUP BY t.id, t.name
                ORDER BY resource_count DESC, %s
                LIMIT :size
                """.formatted(NAME_ORDER);
        return jdbcTemplate.query(sql, new MapSqlParameterSource("size", size), (resultSet, rowNumber) -> new PopularTopic(
                topicOf(resultSet),
                resultSet.getLong("resource_count")
        ));
    }

    private ResourceTopic topicOf(ResultSet resultSet) throws SQLException {
        return new ResourceTopic(resultSet.getString("id"), resultSet.getString("name"));
    }
}
