package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Repository
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
    private final TransactionTemplate readOnlyTransaction;
    private final Semaphore slots;

    public JdbcResourceSearchRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            ResourceSearchProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
        this.slots = new Semaphore(properties.maxConcurrent(), true);
    }

    /**
     * 조각 전체를 읽는 무거운 조회라 두 가지로 막는다.
     * 동시에 도는 조회 수를 제한해 검색이 커넥션 풀을 전부 차지하지 못하게 하고,
     * 제한 시간을 넘긴 조회는 DB가 중단하게 해 느려진 조회가 끝까지 돌며 DB를 붙잡지 않게 한다.
     * 자리를 얻은 뒤에 커넥션을 잡도록 트랜잭션을 메서드 안에서 연다.
     */
    @Override
    public List<ResourceSearchCandidate> findNearest(
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit
    ) {
        validateVector(queryVector);
        acquireSlot();
        try {
            return readOnlyTransaction.execute(
                    status -> findNearestWithinTimeout(queryVector, modelId, track, candidateLimit)
            );
        } finally {
            slots.release();
        }
    }

    private void acquireSlot() {
        try {
            if (!slots.tryAcquire(properties.acquireTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                throw new ServiceUnavailableException(ResourceSearchErrorCode.RESOURCE_SEARCH_BUSY);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException(ResourceSearchErrorCode.RESOURCE_SEARCH_BUSY, exception);
        }
    }

    // 제한 시간은 이 트랜잭션에만 건다. 바깥 트랜잭션에 섞여 불렸을 때 호출자가 걸어 둔 값을 지우지 않도록 조회 전 값으로 되돌린다.
    private List<ResourceSearchCandidate> findNearestWithinTimeout(
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit
    ) {
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

        String previousTimeout = jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT current_setting('statement_timeout')", String.class);
        setLocalStatementTimeout(properties.queryTimeout().toMillis() + "ms");
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
        setLocalStatementTimeout(previousTimeout);
        return candidates;
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

    // set_config의 세 번째 인자가 true면 SET LOCAL과 같이 이 트랜잭션에만 적용된다.
    private void setLocalStatementTimeout(String value) {
        jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT set_config('statement_timeout', ?, true)", String.class, value);
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
