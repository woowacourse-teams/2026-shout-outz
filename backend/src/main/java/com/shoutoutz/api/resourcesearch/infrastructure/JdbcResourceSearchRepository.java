package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JdbcResourceSearchRepository implements ResourceSearchRepository {

    /**
     * 관리자가 받아들인 필요 없어요 제보가 있는 자료(별칭 d)를 뺀다. 프로젝트 기술 이야기의 TeamResourceSql 과 같은 조건이다.
     */
    public static final String NOT_REMOVED_BY_REPORT = """
              AND NOT EXISTS (
                  SELECT 1
                  FROM resource_reports report
                  WHERE report.kind = 'REMOVE'
                    AND report.status = 'ACCEPTED'
                    AND report.resource_source = d.source
                    AND report.resource_external_key = d.external_key
              )
            """;

    private static final int VECTOR_DIMENSIONS = 1024;

    // PostgreSQL이 statement_timeout으로 쿼리를 중단할 때 내는 SQLSTATE
    private static final String QUERY_CANCELED = "57014";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ResourceSearchProperties properties;

    /**
     * 조각 전체를 읽는 무거운 조회라 제한 시간을 건다. 느려진 조회가 끝까지 돌며 DB를 붙잡지 않게 한다.
     * SET LOCAL은 트랜잭션 안에서만 적용되므로 이 조회만 감싸는 트랜잭션을 두고, 바깥 트랜잭션에 섞여 불려도
     * 다음 쿼리에 남지 않도록 조회 뒤에 되돌린다.
     */
    @Override
    @Transactional(readOnly = true)
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
                """ + NOT_REMOVED_BY_REPORT;
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

        jdbcTemplate.getJdbcTemplate().execute("SET LOCAL statement_timeout = " + properties.queryTimeout().toMillis());
        List<ResourceSearchCandidate> candidates;
        try {
            candidates = jdbcTemplate.query(
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
                            ResourceTrack.from(resultSet.getString("track")),
                            resultSet.getString("url"),
                            new MatchedChunk(
                                    resultSet.getLong("chunk_id"),
                                    resultSet.getString("chunk_kind"),
                                    resultSet.getString("chunk_text"),
                                    resultSet.getDouble("similarity_score")
                            )
                    )
            );
        } catch (DataAccessException exception) {
            if (isQueryCanceled(exception)) {
                throw new ServiceUnavailableException(ResourceSearchErrorCode.RESOURCE_SEARCH_TIMEOUT, exception);
            }
            throw exception;
        }
        jdbcTemplate.getJdbcTemplate().execute("SET LOCAL statement_timeout TO DEFAULT");
        return candidates;
    }

    private static boolean isQueryCanceled(DataAccessException exception) {
        if (exception.getRootCause() instanceof SQLException sqlException) {
            return QUERY_CANCELED.equals(sqlException.getSQLState());
        }
        return false;
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
