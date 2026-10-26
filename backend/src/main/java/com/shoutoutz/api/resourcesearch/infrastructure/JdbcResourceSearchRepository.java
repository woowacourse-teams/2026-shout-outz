package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcResourceSearchRepository implements ResourceSearchRepository {

    private static final int VECTOR_DIMENSIONS = 1024;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ResourceSearchCandidate> findNearest(
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit
    ) {
        validateVector(queryVector);
        String sql = """
                SELECT d.id AS document_id, d.external_key, d.project_id, d.repo,
                       d.title, d.summary, d.source, d.track, d.url,
                       c.id AS chunk_id, c.chunk_kind, c.chunk_text,
                       1 - (c.embedding <=> CAST(:queryVector AS vector)) AS similarity_score
                FROM resource_chunks c
                JOIN resource_documents d ON d.id = c.document_id
                WHERE c.deleted_at IS NULL
                  AND d.deleted_at IS NULL
                  AND c.embedding_model = :modelId
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("queryVector", Arrays.toString(queryVector))
                .addValue("modelId", modelId)
                .addValue("candidateLimit", candidateLimit);
        if (track != null && track != ResourceTrack.ALL) {
            sql += " AND (c.track = :track OR c.track = :commonTrack OR c.track IS NULL)";
            parameters.addValue("track", track.name())
                    .addValue("commonTrack", ResourceTrack.ALL.name());
        }
        sql += " ORDER BY similarity_score DESC, c.id LIMIT :candidateLimit";

        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new ResourceSearchCandidate(
                        resultSet.getLong("document_id"),
                        resultSet.getString("external_key"),
                        resultSet.getObject("project_id", Long.class),
                        resultSet.getString("repo"),
                        resultSet.getString("title"),
                        resultSet.getString("summary"),
                        resultSet.getString("source"),
                        toResourceTrack(resultSet.getString("track")),
                        resultSet.getString("url"),
                        new MatchedChunk(
                                resultSet.getLong("chunk_id"),
                                resultSet.getString("chunk_kind"),
                                resultSet.getString("chunk_text"),
                                resultSet.getDouble("similarity_score")
                        )
                )
        );
    }

    private ResourceTrack toResourceTrack(String track) {
        if (track == null) {
            return null;
        }
        return ResourceTrack.valueOf(track);
    }

    private void validateVector(float[] vector) {
        if (vector == null || vector.length != VECTOR_DIMENSIONS) {
            throw new IllegalArgumentException("검색 벡터는 1024차원이어야 합니다.");
        }
        boolean hasNonZeroValue = false;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("검색 벡터에는 유한한 값만 사용할 수 있습니다.");
            }
            if (value != 0) {
                hasNonZeroValue = true;
            }
        }
        if (!hasNonZeroValue) {
            throw new IllegalArgumentException("검색 벡터는 영벡터일 수 없습니다.");
        }
    }
}
