package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    /**
     * HNSW 인덱스(resource_chunks_embedding_idx)로 가까운 조각을 찾게 한다. 이 트랜잭션 안에서만 적용된다.
     * <ul>
     *     <li>iterative_scan: 트랙, 삭제, 제보 조건에 걸러져도 후보 수(candidateLimit)를 채울 때까지 인덱스를 더 읽는다.
     *     끄면 ef_search(기본 40)개까지만 읽어 후보가 모자란다. relaxed_order 라 순서가 조금 어긋날 수 있어 바깥에서 다시 정렬한다.</li>
     *     <li>enable_sort: 벡터가 TOAST 에 있어 플래너가 순차 스캔 비용을 실제보다 낮게 본다. 끄지 않으면 트랙 필터가 있거나
     *     후보 수가 클 때 조각 전체를 읽고 정렬하는 계획을 고른다.</li>
     * </ul>
     */
    private static final String USE_VECTOR_INDEX =
            "SET LOCAL hnsw.iterative_scan = relaxed_order; SET LOCAL enable_sort = off";

    /** 같은 트랜잭션에서 이어지는 다른 쿼리(예: 프로젝트 검색의 목록 조회)가 정렬을 못 쓰지 않도록 되돌린다. */
    private static final String RESTORE_PLANNER_SETTINGS =
            "SET LOCAL hnsw.iterative_scan TO DEFAULT; SET LOCAL enable_sort TO DEFAULT";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 질문 벡터와 코사인 거리가 가까운 조각을 candidateLimit 개까지 찾는다.
     * HNSW 인덱스는 거리 연산자({@code <=>}) 오름차순 정렬에만 쓰이므로 1 - 거리(유사도) 내림차순으로 정렬하지 않는다.
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
                WITH candidates AS MATERIALIZED (
                    SELECT d.id AS document_id, d.external_key, d.project_id, d.repo,
                           d.title, d.summary, d.source, d.track, d.url,
                           c.id AS chunk_id, c.chunk_kind, c.chunk_text,
                           c.embedding <=> CAST(:queryVector AS vector) AS distance
                    FROM resource_chunks c
                    JOIN resource_documents d ON d.id = c.document_id
                    WHERE c.deleted_at IS NULL
                      AND d.deleted_at IS NULL
                      AND c.embedding_model = :modelId
                """ + NOT_REMOVED_BY_REPORT;
        // 문자열이 아니라 vector 로 묶이게 타입을 비워 보낸다. 일반 계획(generic plan)에서 행마다 문자열을 다시 읽지 않는다.
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("queryVector", Arrays.toString(queryVector), Types.OTHER)
                .addValue("modelId", modelId)
                .addValue("candidateLimit", candidateLimit);
        if (track != null && track != ResourceTrack.ALL) {
            sql += " AND (c.track = :track OR c.track = :commonTrack OR c.track IS NULL)";
            parameters.addValue("track", track.name())
                    .addValue("commonTrack", ResourceTrack.ALL.name());
        }
        sql += """
                    ORDER BY distance
                    LIMIT :candidateLimit
                )
                SELECT candidates.*, 1 - distance AS similarity_score
                FROM candidates
                ORDER BY distance, chunk_id
                """;

        jdbcTemplate.getJdbcTemplate().execute(USE_VECTOR_INDEX);
        List<ResourceSearchCandidate> candidates = jdbcTemplate.query(
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
        jdbcTemplate.getJdbcTemplate().execute(RESTORE_PLANNER_SETTINGS);
        return candidates;
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
