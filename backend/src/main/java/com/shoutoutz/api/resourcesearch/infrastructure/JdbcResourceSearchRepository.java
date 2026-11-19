package com.shoutoutz.api.resourcesearch.infrastructure;

import com.shoutoutz.api.common.exception.custom.ServiceUnavailableException;
import com.shoutoutz.api.resourcesearch.application.ResourceSearchRepository;
import com.shoutoutz.api.resourcesearch.application.dto.LexicalChunk;
import com.shoutoutz.api.resourcesearch.application.dto.MatchedChunk;
import com.shoutoutz.api.resourcesearch.application.dto.RankedResource;
import com.shoutoutz.api.resourcesearch.application.dto.ResourceSearchCandidate;
import com.shoutoutz.api.resourcesearch.application.dto.SearchCandidates;
import com.shoutoutz.api.resourcesearch.domain.ResourceSearchErrorCode;
import com.shoutoutz.api.resourcesearch.domain.ResourceTrack;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

    /**
     * 벡터 조회가 halfvec 인덱스(resource_chunks_embedding_half_idx)를 쓰게 하는 설정이다. 트랜잭션 안에서만 적용된다.
     * <ul>
     *     <li>ef_search 1000: 조각 400개를 뽑을 때 검색어 40개 모두에서 가까운 60개가 전체 비교와 같아진 값이다.
     *     400이면 40개 중 33개만 같았고, 한 건의 조회 시간은 약 3ms 차이였다.</li>
     *     <li>iterative_scan: 트랙, 삭제, 제보 조건에 걸러진 만큼 인덱스를 더 읽어 후보 수를 채운다.
     *     relaxed_order 는 strict_order 보다 놓치는 조각이 적고, 어긋난 순서는 바깥 쿼리에서 다시 정렬한다.</li>
     *     <li>enable_sort off: 끄지 않으면 플래너가 조각 전체를 읽고 정렬하는 계획을 고른다.</li>
     *     <li>jit off: 정렬을 끄면 마지막 정렬 단계의 예상 비용이 크게 잡혀 JIT 컴파일이 돈다. 400행을 다루는 쿼리라 얻는 것이 없고
     *     컴파일에만 약 250ms가 걸렸다.</li>
     * </ul>
     */
    private static final String USE_VECTOR_INDEX = """
            SET LOCAL hnsw.ef_search = 1000;
            SET LOCAL hnsw.iterative_scan = relaxed_order;
            SET LOCAL enable_sort = off;
            SET LOCAL jit = off
            """;

    // 조회하는 동안 바꾸는 설정. 호출한 쪽의 트랜잭션에 참여했을 때 호출자가 걸어 둔 값을 지우지 않도록 조회 전 값으로 되돌린다.
    private static final List<String> SEARCH_SETTINGS =
            List.of("statement_timeout", "hnsw.ef_search", "hnsw.iterative_scan", "enable_sort", "jit");

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
     * DB 메모리가 모자라면 인덱스 조회도 느려지므로 두 가지로 막는다.
     * 동시에 도는 조회 수를 제한해 검색이 커넥션 풀을 전부 차지하지 못하게 하고,
     * 제한 시간을 넘긴 조회는 DB가 중단하게 해 느려진 조회가 끝까지 돌며 DB를 붙잡지 않게 한다.
     * 자리를 얻은 뒤에 커넥션을 잡도록 트랜잭션을 메서드 안에서 연다.
     * 단어 검색이 찾은 조각을 따로 조회하지 않고 같은 쿼리에 묶는다. 조회가 둘이면 자리와 커넥션을 두 번 기다릴 수 있다.
     */
    @Override
    public SearchCandidates findCandidates(
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit,
            List<Long> lexicalChunkIds
    ) {
        validateVector(queryVector);
        acquireSlot();
        try {
            return readOnlyTransaction.execute(
                    status -> findCandidatesWithinTimeout(queryVector, modelId, track, candidateLimit, lexicalChunkIds)
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

    // 제한 시간과 탐색 설정은 이 트랜잭션에만 건다.
    private SearchCandidates findCandidatesWithinTimeout(
            float[] queryVector,
            String modelId,
            ResourceTrack track,
            int candidateLimit,
            List<Long> lexicalChunkIds
    ) {
        String columns = """
                SELECT d.id AS document_id, d.external_key, d.project_id, d.repo,
                       d.title, d.summary, d.source, d.track, d.url,
                       c.id AS chunk_id, c.chunk_kind, c.chunk_text,
                       c.embedding::halfvec(1024) <=> CAST(:queryVector AS halfvec(1024)) AS distance
                FROM resource_chunks c
                JOIN resource_documents d ON d.id = c.document_id
                WHERE c.deleted_at IS NULL
                  AND d.deleted_at IS NULL
                  AND c.embedding_model = :modelId
                """ + NOT_REMOVED_BY_REPORT;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("queryVector", Arrays.toString(queryVector))
                .addValue("modelId", modelId)
                .addValue("candidateLimit", candidateLimit)
                .addValue("lexicalChunkIds", toArrayLiteral(lexicalChunkIds));
        String trackCondition = "";
        if (track != null && track != ResourceTrack.ALL) {
            trackCondition = " AND (c.track = :track OR c.track = :commonTrack OR c.track IS NULL)";
            parameters.addValue("track", track.name())
                    .addValue("commonTrack", ResourceTrack.ALL.name());
        }
        // 인덱스는 embedding::halfvec(1024) 식의 거리를 오름차순으로 정렬할 때만 쓰인다. 식이 다르거나 유사도 내림차순이면 조각 전체를 읽는다.
        // 단어 검색이 찾은 조각은 색인을 만들 때 이미 트랙으로 걸렀으므로 트랙 조건을 다시 걸지 않는다.
        String sql = "WITH nearest AS MATERIALIZED (" + columns + trackCondition + """
                    ORDER BY distance
                    LIMIT :candidateLimit
                ), lexical_only AS (
                """ + columns + """
                      AND c.id = ANY(CAST(:lexicalChunkIds AS bigint[]))
                      AND NOT EXISTS (SELECT 1 FROM nearest WHERE nearest.chunk_id = c.id)
                )
                SELECT nearest.*, 1 - distance AS similarity_score, TRUE AS is_nearest FROM nearest
                UNION ALL
                SELECT lexical_only.*, 1 - distance AS similarity_score, FALSE AS is_nearest FROM lexical_only
                ORDER BY is_nearest DESC, distance, chunk_id
                """;

        List<String> previousSettings = currentSettings();
        setLocal("statement_timeout", properties.queryTimeout().toMillis() + "ms");
        jdbcTemplate.getJdbcTemplate().execute(USE_VECTOR_INDEX);
        List<ResourceSearchCandidate> nearest = new ArrayList<>();
        List<ResourceSearchCandidate> lexicalOnly = new ArrayList<>();
        try {
            jdbcTemplate.query(sql, parameters, resultSet -> {
                ResourceSearchCandidate candidate = new ResourceSearchCandidate(
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
                if (resultSet.getBoolean("is_nearest")) {
                    nearest.add(candidate);
                    return;
                }
                lexicalOnly.add(candidate);
            });
        } catch (DataAccessException exception) {
            if (isQueryCanceled(exception)) {
                throw new ServiceUnavailableException(ResourceSearchErrorCode.RESOURCE_SEARCH_TIMEOUT, exception);
            }
            throw exception;
        }
        for (int index = 0; index < SEARCH_SETTINGS.size(); index++) {
            setLocal(SEARCH_SETTINGS.get(index), previousSettings.get(index));
        }
        return new SearchCandidates(nearest, lexicalOnly);
    }

    private static String toArrayLiteral(List<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "{", "}"));
    }

    /**
     * 렉시컬 색인에 넣을 조각을 읽는다. 벡터 검색과 같은 자료가 검색되도록 삭제, 모델, 제보 조건을 똑같이 건다.
     * 본문이 공백뿐인 조각은 찾을 단어가 없어 뺀다. 개행과 탭도 공백으로 보도록 LexicalChunk와 같은 isBlank 기준으로 거른다.
     * 트랙 값이 잘못된 조각은 그 조각만 빼고 경고를 남긴다. 조각 하나 때문에 색인 적재 전체가 실패하면 서버가 뜨지 못한다.
     */
    @Override
    public List<LexicalChunk> findLexicalChunks(String modelId) {
        String sql = """
                SELECT c.id AS chunk_id, c.chunk_text, c.track, d.id AS document_id, d.project_id, d.source
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
                chunks.add(new LexicalChunk(
                        chunkId,
                        text,
                        ResourceTrack.from(resultSet.getString("track")),
                        new RankedResource(
                                resultSet.getLong("document_id"),
                                resultSet.getObject("project_id", Long.class),
                                resultSet.getString("source")
                        )
                ));
            } catch (IllegalArgumentException exception) {
                log.warn("Lexical chunk skipped: chunkId={}, reason={}", chunkId, exception.getMessage());
            }
        });
        return List.copyOf(chunks);
    }

    // pgvector 를 아직 쓰지 않은 연결에서는 hnsw 설정이 없어 null 이 온다.
    private List<String> currentSettings() {
        List<String> values = new ArrayList<>();
        for (String name : SEARCH_SETTINGS) {
            values.add(jdbcTemplate.getJdbcTemplate()
                    .queryForObject("SELECT current_setting(?, true)", String.class, name));
        }
        return values;
    }

    // set_config의 세 번째 인자가 true면 SET LOCAL과 같이 이 트랜잭션에만 적용된다. 값이 null 이면 기본값으로 돌아간다.
    private void setLocal(String name, String value) {
        jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT set_config(?, ?, true)", String.class, name, value);
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
