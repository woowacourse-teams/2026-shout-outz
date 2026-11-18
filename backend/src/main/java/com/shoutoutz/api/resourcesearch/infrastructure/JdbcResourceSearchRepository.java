package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
     * 벡터 조회가 HNSW 인덱스(resource_chunks_embedding_idx)를 쓰게 하는 설정이다. 트랜잭션 안에서만 적용된다.
     * <ul>
     *     <li>ef_search 400: 평가 검색어 8개에서 후보 20개와 60개가 전체 비교와 같아진 값이다. 기본값 40은 약 91~94%만 같았다.</li>
     *     <li>iterative_scan: 트랙, 삭제, 제보 조건에 걸러진 만큼 인덱스를 더 읽어 후보 수를 채운다.
     *     relaxed_order 는 strict_order 보다 놓치는 조각이 적고, 어긋난 순서는 바깥 쿼리에서 다시 정렬한다.</li>
     *     <li>enable_sort off: 끄지 않으면 플래너가 조각 전체를 읽고 정렬하는 계획을 고른다.</li>
     * </ul>
     */
    private static final String USE_VECTOR_INDEX = """
            SET LOCAL hnsw.ef_search = 400;
            SET LOCAL hnsw.iterative_scan = relaxed_order;
            SET LOCAL enable_sort = off
            """;

    /**
     * 호출한 쪽의 트랜잭션에 참여했을 때 뒤따르는 쿼리의 실행 계획이 바뀌지 않도록 되돌린다.
     */
    private static final String RESTORE_SETTINGS = """
            SET LOCAL hnsw.ef_search TO DEFAULT;
            SET LOCAL hnsw.iterative_scan TO DEFAULT;
            SET LOCAL enable_sort TO DEFAULT
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * HNSW 인덱스는 거리 연산자의 오름차순 정렬에만 쓰인다. 유사도(1 - 거리)의 내림차순으로 정렬하면 조각 전체를 읽는다.
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
                WITH nearest AS MATERIALIZED (
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
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("queryVector", Arrays.toString(queryVector))
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
                SELECT nearest.*, 1 - distance AS similarity_score
                FROM nearest
                ORDER BY distance, chunk_id
                """;

        jdbcTemplate.getJdbcTemplate().execute(USE_VECTOR_INDEX);
        List<ResourceSearchCandidate> candidates = jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> toCandidate(resultSet)
        );
        jdbcTemplate.getJdbcTemplate().execute(RESTORE_SETTINGS);

        return candidates;
    }

    /**
     * 렉시컬 검색으로 찾은 조각의 문서 정보와, 질문 벡터와의 코사인 유사도를 읽는다.
     * 색인을 만든 뒤 지워졌거나 제보로 빠진 자료가 응답에 섞이지 않도록 벡터 검색과 같은 조건을 다시 건다.
     */
    @Override
    public List<ResourceSearchCandidate> findByChunkIds(List<Long> chunkIds, float[] queryVector, String modelId) {
        validateVector(queryVector);
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        String sql = """
                SELECT d.id AS document_id, d.external_key, d.project_id, d.repo,
                       d.title, d.summary, d.source, d.track, d.url,
                       c.id AS chunk_id, c.chunk_kind, c.chunk_text,
                       1 - (c.embedding <=> CAST(:queryVector AS vector)) AS similarity_score
                FROM resource_chunks c
                JOIN resource_documents d ON d.id = c.document_id
                WHERE c.id IN (:chunkIds)
                  AND c.deleted_at IS NULL
                  AND d.deleted_at IS NULL
                  AND c.embedding_model = :modelId
                """ + NOT_REMOVED_BY_REPORT + " ORDER BY c.id";
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("chunkIds", chunkIds)
                .addValue("queryVector", Arrays.toString(queryVector))
                .addValue("modelId", modelId);

        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> toCandidate(resultSet));
    }

    /**
     * 렉시컬 색인에 넣을 조각을 읽는다. 벡터 검색과 같은 자료가 검색되도록 삭제, 모델, 제보 조건을 똑같이 건다.
     * 본문이 공백뿐인 조각은 찾을 단어가 없어 뺀다. 개행과 탭도 공백으로 보도록 LexicalChunk와 같은 isBlank 기준으로 거른다.
     * 트랙 값이 잘못된 조각은 그 조각만 빼고 경고를 남긴다. 조각 하나 때문에 색인 적재 전체가 실패하면 서버가 뜨지 못한다.
     */
    @Override
    public List<LexicalChunk> findLexicalChunks(String modelId) {
        String sql = """
                SELECT c.id AS chunk_id, c.chunk_text, c.track
                FROM resource_chunks c
                JOIN resource_documents d ON d.id = c.document_id
                WHERE c.deleted_at IS NULL
                  AND d.deleted_at IS NULL
                  AND c.embedding_model = :modelId
                """ + NOT_REMOVED_BY_REPORT + " ORDER BY c.id";

        List<LexicalChunk> chunks = new ArrayList<>();
        jdbcTemplate.query(sql, new MapSqlParameterSource("modelId", modelId), resultSet -> {
            String text = resultSet.getString("chunk_text");
            if (text == null || text.isBlank()) {
                return;
            }
            long chunkId = resultSet.getLong("chunk_id");
            try {
                chunks.add(new LexicalChunk(chunkId, text, ResourceTrack.from(resultSet.getString("track"))));
            } catch (IllegalArgumentException exception) {
                log.warn("Lexical chunk skipped: chunkId={}, reason={}", chunkId, exception.getMessage());
            }
        });
        return List.copyOf(chunks);
    }

    private static ResourceSearchCandidate toCandidate(ResultSet resultSet) throws SQLException {
        return new ResourceSearchCandidate(
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
        );
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
