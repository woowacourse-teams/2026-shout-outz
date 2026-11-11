package com.shoutoutz.api.resourcesearch.infrastructure;

import static com.shoutoutz.api.resourcesearch.infrastructure.ResourceCardQueryRepositoryImpl.CARD_COLUMNS;
import static com.shoutoutz.api.resourcesearch.infrastructure.ResourceCardQueryRepositoryImpl.VISIBLE_PROJECT_JOIN;

import com.shoutoutz.api.resourcesearch.application.ResourceDetailQueryRepository;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceCard;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceDetail;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceExperience;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceLinkPreview;
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
                SELECT %s, d.content_type, d.key_points, d.tech_tags, t.id AS topic_id, t.name AS topic_name,
                       preview.title AS preview_title, preview.description AS preview_description,
                       preview.image_url AS preview_image_url, preview.site_name AS preview_site_name
                FROM resource_documents d
                %s
                LEFT JOIN resource_topic_catalog t ON t.id = d.topic
                LEFT JOIN resource_link_preview_refs preview_ref ON preview_ref.document_id = d.id
                LEFT JOIN link_preview_cache preview ON preview.id = preview_ref.cache_id
                                                    AND preview.status = 'READY'
                                                    AND preview.url = d.url
                WHERE d.id = :resourceId
                  AND d.deleted_at IS NULL
                """.formatted(CARD_COLUMNS, VISIBLE_PROJECT_JOIN) + JdbcResourceSearchRepository.NOT_REMOVED_BY_REPORT;
        MapSqlParameterSource parameters = new MapSqlParameterSource("resourceId", resourceId);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> new DetailRow(
                        ResourceCardQueryRepositoryImpl.mapCard(resultSet),
                        resultSet.getString("content_type"),
                        topicOf(resultSet),
                        textsOf(resultSet.getArray("key_points")),
                        textsOf(resultSet.getArray("tech_tags")),
                        linkPreviewOf(resultSet)
                ))
                .stream()
                .findFirst()
                .map(row -> row.toDetail(findExperiences(resourceId)));
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

    private ResourceLinkPreview linkPreviewOf(ResultSet resultSet) throws SQLException {
        String title = resultSet.getString("preview_title");
        String imageUrl = resultSet.getString("preview_image_url");
        if (title == null && imageUrl == null) {
            return null;
        }
        return new ResourceLinkPreview(
                title,
                resultSet.getString("preview_description"),
                imageUrl,
                resultSet.getString("preview_site_name")
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

    private record DetailRow(
            ResourceCard card,
            String contentType,
            ResourceTopic topic,
            List<String> keyPoints,
            List<String> techTags,
            ResourceLinkPreview linkPreview
    ) {

        private ResourceDetail toDetail(List<ResourceExperience> experiences) {
            return new ResourceDetail(
                    card,
                    contentType,
                    topic,
                    keyPoints,
                    techTags,
                    experiences,
                    linkPreview
            );
        }
    }
}
